import { api, WorkOrderDetail } from "../../utils/api"
import { localImage } from "../../utils/image"

interface LogImage {
  id: number
  accessUrl: string
}

interface EventPhotoView {
  id: number
  url: string
}

interface ProcessPhoto {
  id: number
  url: string
}

interface MapMarker {
  id: number
  latitude: number
  longitude: number
  width: number
  height: number
  iconPath: string
  callout?: Record<string, unknown>
}

type LogRow = WorkOrderDetail["logs"][number] & {
  actionText: string
  images: LogImage[]
  canDelete: boolean
}

type DetailView = Omit<WorkOrderDetail, "eventPhotos"> & {
  statusText: string
  logRows: LogRow[]
  eventPhotos: EventPhotoView[]
  hasLocation: boolean
  markers: MapMarker[]
}

const STATUS_TEXT: Record<string, string> = {
  PENDING_ACCEPT: "待接单",
  PROCESSING: "处理中",
  PENDING_LEADER_REVIEW: "待组长复核",
  PENDING_ACCEPTANCE: "待验收",
  COMPLETED: "已完成",
}

const ACTION_TEXT: Record<string, string> = {
  CREATE: "派单给组长",
  ASSIGN: "组长分配组员",
  ACCEPT: "接单",
  CANCEL_ACCEPT: "撤销接单",
  PROCESS: "处理记录",
  SUBMIT: "提交结果",
  WITHDRAW_SUBMIT: "撤回处理结果",
  LEADER_PASS: "组长复核通过",
  LEADER_REJECT: "组长复核退回",
  REASSIGN: "管理员改派",
}

const DEGRADE_TEXT: Record<string, string> = {
  NO_IMAGE: "没有可用的现场照片，请先上传照片",
  IMAGE_UNREADABLE: "照片无法读取或格式不支持，请换一张重传",
  IMAGE_TOO_LARGE: "照片过大，服务器无法处理，请压缩后重传",
  LLM_RATE_LIMITED: "AI 服务当前繁忙，请稍后重试",
  QUOTA_EXCEEDED: "AI 服务额度不足，请稍后重试或手动填写",
  TIMEOUT: "AI 服务响应超时，请稍后重试",
  CIRCUIT_OPEN: "AI 服务熔断中，请稍后重试",
  RATE_LIMITED: "AI 调用过于频繁，请稍后重试",
  PARSE_FAILED: "AI 返回结果无法解析，请重试",
  EMPTY_RESPONSE: "AI 未返回内容，请重试",
}

function absoluteUrl(url: string) {
  if (!url || url.startsWith("http")) {
    return url
  }
  return getApp<IAppOption>().globalData.baseUrl + url
}

Page({
  data: {
    detail: null as null | DetailView,
    loading: true,
    loadError: "",
    content: "",
    photos: [] as string[],
    photoIds: [] as number[],
    submitting: false,
    submitError: "",
    processContent: "",
    processPhotos: [] as ProcessPhoto[],
    processSaving: false,
    processError: "",
    aiLoading: false,
    aiError: "",
    isLeader: false,
    myUserId: 0,
    teamMembers: [] as { id: number; nickname: string }[],
    assignIndex: -1,
    assigning: false,
    assignError: "",
    acceptError: "",
    leaderRemark: "",
    reviewSubmitting: false,
    reviewError: "",
    undoLoading: false,
    withdrawLoading: false,
    undoError: "",
    deletingLogId: 0,
    logError: "",
  },

  orderId: 0,
  thumbErrorShown: false,
  processSignature: "",
  processPending: false,

  onLoad(options: Record<string, string>) {
    this.orderId = Number(options.id)
    this.load()
  },

  async load(silent = false) {
    if (!silent) {
      this.setData({ loading: true, loadError: "" })
    }
    try {
      const detail = await api.workOrderDetail(this.orderId)
      await this.loadLeaderContext(detail)
      const logRows = await this.buildLogRows(detail.logs || [], detail)
      const event = detail.event
      const latitude = event && event.latitude ? Number(event.latitude) : 0
      const longitude = event && event.longitude ? Number(event.longitude) : 0
      const hasLocation = latitude !== 0 && longitude !== 0
      const markers: MapMarker[] = hasLocation
        ? [{
            id: 1,
            latitude,
            longitude,
            width: 28,
            height: 36,
            iconPath: "/assets/location-pin.png",
            callout: {
              content: event!.address || "事发位置",
              color: "#1f2937",
              fontSize: 12,
              borderRadius: 6,
              padding: 6,
              display: "ALWAYS",
            },
          }]
        : []
      const eventPhotos: EventPhotoView[] = await Promise.all(
        (detail.eventPhotos || []).map(async (photo) => ({
          id: photo.id,
          url: await localImage(absoluteUrl(photo.accessUrl)),
        })),
      )
      this.setData({
        detail: {
          ...detail,
          statusText: STATUS_TEXT[detail.status] || detail.status,
          logRows,
          eventPhotos,
          hasLocation,
          markers,
        },
      })
      this.syncProcessEditor(logRows)
    } catch (error) {
      this.setData({ loadError: (error as Error).message })
    } finally {
      if (!silent) {
        this.setData({ loading: false })
      }
    }
  },

  /**
   * 用本人最新一条过程记录回填编辑区，保证重新进入工单时能看到上次自动保存的内容。
   */
  syncProcessEditor(logRows: LogRow[]) {
    const mine = logRows.filter((row) => row.action === "PROCESS" && row.operatorId === this.data.myUserId)
    const latest = mine.length ? mine[mine.length - 1] : null
    const content = latest && latest.content ? latest.content : ""
    const photos: ProcessPhoto[] = latest
      ? latest.images.map((image) => ({ id: image.id, url: image.accessUrl }))
      : []
    this.setData({ processContent: content, processPhotos: photos })
    this.processSignature = this.processSignatureOf(content, photos)
  },

  processSignatureOf(content: string, photos: ProcessPhoto[]) {
    return content.trim() + "|" + photos.map((photo) => photo.id).join(",")
  },

  /**
   * 自动保存过程记录：内容或照片变化时更新同一条，全部清空时删除该记录。
   * 保存中收到的改动会在本次结束后自动再保存一次。
   */
  async saveProcessRecord() {
    const content = this.data.processContent.trim()
    const photos = this.data.processPhotos
    if (this.processSignatureOf(content, photos) === this.processSignature) {
      return
    }
    if (this.data.processSaving) {
      this.processPending = true
      return
    }
    this.setData({ processSaving: true, processError: "" })
    try {
      await api.saveWorkOrderProcess(this.orderId, content, photos.map((photo) => photo.id))
      await this.load(true)
    } catch (error) {
      this.setData({ processError: (error as Error).message })
    } finally {
      this.setData({ processSaving: false })
      if (this.processPending) {
        this.processPending = false
        this.saveProcessRecord()
      }
    }
  },

  async buildLogRows(logs: WorkOrderDetail["logs"], detail: WorkOrderDetail): Promise<LogRow[]> {
    const idLists = logs.map((log) =>
      (log.attachmentIds || "")
        .split(",")
        .map((item) => Number(item.trim()))
        .filter((id) => id > 0),
    )
    const allIds = idLists.reduce<number[]>((acc, ids) => acc.concat(ids), [])
    const uniqueIds = allIds.filter((id, index) => allIds.indexOf(id) === index)
    const imageMap: Record<number, LogImage> = {}
    if (uniqueIds.length > 0) {
      try {
        const attachments = await api.batchAttachments(uniqueIds)
        attachments.forEach((item) => {
          imageMap[item.id] = { id: item.id, accessUrl: absoluteUrl(item.accessUrl) }
        })
      } catch (e) {
        // 附件查询失败时仅展示文字记录，不阻断详情
      }
    }
    return Promise.all(logs.map(async (log, index) => {
      const images = await Promise.all(
        idLists[index]
          .map((id) => imageMap[id])
          .filter((item) => !!item)
          .map(async (item) => ({ id: item.id, accessUrl: await localImage(item.accessUrl) })),
      )
      return {
        ...log,
        actionText: ACTION_TEXT[log.action] || log.action,
        images,
        canDelete: log.action === "PROCESS"
          && detail.status === "PROCESSING"
          && detail.assigneeId === this.data.myUserId,
      }
    }))
  },

  /**
   * 组长身份校验：只有组长展示分配组员与组内复核入口。
   */
  async loadLeaderContext(detail: WorkOrderDetail) {
    const role = (wx.getStorageSync("role") as string) || getApp<IAppOption>().globalData.role || ""
    if (role !== "WORKER") {
      this.setData({ isLeader: false, teamMembers: [] })
      return
    }
    try {
      const profile = await api.workerProfile()
      this.setData({ myUserId: profile.userId || 0 })
      if (profile.workerType !== "LEADER") {
        this.setData({ isLeader: false, teamMembers: [] })
        return
      }
      const members = await api.myTeam()
      this.setData({
        isLeader: true,
        teamMembers: members.map((item) => ({ id: item.id, nickname: item.nickname })),
        assignIndex: detail.status === "PENDING_ACCEPT" ? 0 : this.data.assignIndex,
      })
    } catch (e) {
      this.setData({ isLeader: false, teamMembers: [] })
    }
  },

  onAssignChange(e: any) {
    this.setData({ assignIndex: Number(e.detail.value) })
  },

  async assignToMember() {
    const member = this.data.teamMembers[this.data.assignIndex]
    if (!member) {
      wx.showToast({ title: "请选择组员", icon: "none" })
      return
    }
    this.setData({ assigning: true, assignError: "" })
    try {
      await api.assignWorkOrder(this.orderId, member.id)
      wx.showToast({ title: "已分配给" + member.nickname, icon: "success" })
      await this.load()
    } catch (error) {
      this.setData({ assignError: (error as Error).message })
    } finally {
      this.setData({ assigning: false })
    }
  },

  onLeaderRemarkInput(e: any) {
    this.setData({ leaderRemark: e.detail.value })
  },

  async reviewLeader(e: any) {
    const pass = e.currentTarget.dataset.pass === "true"
    const remark = this.data.leaderRemark.trim()
    if (!pass && remark.length < 2) {
      this.setData({ reviewError: "退回时请填写退回原因" })
      return
    }
    if (pass) {
      const confirmed = await new Promise<boolean>((resolve) => {
        wx.showModal({
          title: "复核通过",
          content: "通过后工单将提交管理员验收，确认整改结果已达标？",
          confirmText: "确认通过",
          success: (res) => resolve(!!res.confirm),
          fail: () => resolve(false),
        })
      })
      if (!confirmed) {
        return
      }
    }
    this.setData({ reviewSubmitting: true, reviewError: "" })
    try {
      await api.leaderReviewWorkOrder(this.orderId, pass, remark)
      wx.showToast({ title: pass ? "已提交验收" : "已退回整改", icon: "success" })
      this.setData({ leaderRemark: "" })
      await this.load()
    } catch (error) {
      this.setData({ reviewError: (error as Error).message })
    } finally {
      this.setData({ reviewSubmitting: false })
    }
  },

  ratingStars(value: number) {
    const rating = Math.max(0, Math.min(5, Number(value) || 0))
    return "★★★★★".slice(0, rating) + "☆☆☆☆☆".slice(0, 5 - rating)
  },

  async accept() {
    this.setData({ submitting: true, acceptError: "" })
    try {
      await api.acceptWorkOrder(this.orderId)
      wx.showToast({ title: "接单成功", icon: "success" })
      this.load()
    } catch (error) {
      this.setData({ acceptError: (error as Error).message })
    } finally {
      this.setData({ submitting: false })
    }
  },

  async cancelAccept() {
    const confirmed = await new Promise<boolean>((resolve) => {
      wx.showModal({
        title: "撤销接单",
        content: "撤销后工单回到待接单，由组长重新安排处理人，确认撤销？",
        confirmText: "确认撤销",
        success: (res) => resolve(!!res.confirm),
        fail: () => resolve(false),
      })
    })
    if (!confirmed) {
      return
    }
    this.setData({ undoLoading: true, undoError: "" })
    try {
      await api.cancelAcceptWorkOrder(this.orderId)
      wx.showToast({ title: "已撤销接单", icon: "success" })
      await this.load()
    } catch (error) {
      this.setData({ undoError: (error as Error).message })
    } finally {
      this.setData({ undoLoading: false })
    }
  },

  async withdrawSubmit() {
    const confirmed = await new Promise<boolean>((resolve) => {
      wx.showModal({
        title: "撤回处理结果",
        content: "撤回后工单回到处理中，已提交的结果不再作为验收依据，确认撤回？",
        confirmText: "确认撤回",
        success: (res) => resolve(!!res.confirm),
        fail: () => resolve(false),
      })
    })
    if (!confirmed) {
      return
    }
    this.setData({ withdrawLoading: true, undoError: "" })
    try {
      await api.withdrawWorkOrderResult(this.orderId)
      wx.showToast({ title: "已撤回处理结果", icon: "success" })
      await this.load()
    } catch (error) {
      this.setData({ undoError: (error as Error).message })
    } finally {
      this.setData({ withdrawLoading: false })
    }
  },

  onContentInput(e: any) {
    this.setData({ content: e.detail.value })
  },

  choosePhotos() {
    wx.chooseMedia({
      count: 9 - this.data.photos.length,
      mediaType: ["image"],
      sourceType: ["album", "camera"],
      sizeType: ["compressed"],
      success: (res) => {
        const paths = res.tempFiles.map((file) => file.tempFilePath)
        this.setData({ photos: this.data.photos.concat(paths), aiError: "" })
      },
      fail: () => wx.showToast({ title: "未选择照片", icon: "none" }),
    })
  },

  removePhoto(e: any) {
    const index = Number(e.currentTarget.dataset.index)
    const photos = this.data.photos.slice()
    const photoIds = this.data.photoIds.slice()
    photos.splice(index, 1)
    // 已上传的编号与照片按下标对齐，删除照片时同步移除，避免提交时错位
    photoIds.splice(index, 1)
    this.setData({ photos, photoIds, aiError: "" })
  },

  previewPhoto(e: any) {
    const current = e.currentTarget.dataset.src
    const urls = this.data.photos.length ? this.data.photos : [current]
    wx.previewImage({ current, urls })
  },

  /**
   * 缩略图加载失败时给出提示，便于区分“图片没下载下来”和“样式没渲染出来”。
   */
  onThumbError(e: any) {
    console.warn("缩略图加载失败", e && e.detail)
    if (this.thumbErrorShown) {
      return
    }
    this.thumbErrorShown = true
    wx.showToast({ title: "有缩略图加载失败，可点击查看大图", icon: "none" })
  },

  previewEventPhoto(e: any) {
    const current = e.currentTarget.dataset.src
    const detail = this.data.detail
    const urls = detail ? detail.eventPhotos.map((photo) => photo.url) : [current]
    wx.previewImage({ current, urls })
  },

  /**
   * 调起手机自带地图应用，直接进入导航页。
   */
  openNavigation() {
    const detail = this.data.detail
    const event = detail && detail.event
    if (!detail || !detail.hasLocation || !event) {
      wx.showToast({ title: "该事件没有坐标信息", icon: "none" })
      return
    }
    wx.openLocation({
      latitude: Number(event.latitude),
      longitude: Number(event.longitude),
      name: event.title || "事发位置",
      address: event.address || "",
      scale: 18,
      fail: () => wx.showToast({ title: "打开地图失败，请检查定位权限", icon: "none" }),
    })
  },

  /**
   * 依次上传尚未上传的照片，返回与 photo 数组等长的附件编号。
   */
  async uploadResultPhotos(): Promise<number[]> {
    const ids = this.data.photoIds.slice()
    for (let index = ids.length; index < this.data.photos.length; index++) {
      const uploaded = await api.upload(this.data.photos[index], "WORKORDER_RESULT")
      ids.push(uploaded.id)
    }
    this.setData({ photoIds: ids })
    return ids
  },

  /**
   * AI 生成处理说明：只需 1 张整改后照片，不要求先写文字；生成结果直接回填输入框供修改。
   */
  async generateDraft() {
    if (this.data.photos.length === 0) {
      this.setData({ aiError: "请先上传至少 1 张整改后照片，AI 才能生成处理说明" })
      return
    }
    this.setData({ aiLoading: true, aiError: "" })
    try {
      const ids = await this.uploadResultPhotos()
      const result = await api.generateWorkOrderDraft(this.orderId, ids)
      if (result.degraded || !result.content) {
        const reason = DEGRADE_TEXT[result.degradeReason || ""] || "AI 服务繁忙，请稍后重试或手动填写"
        this.setData({ aiError: reason })
        return
      }
      this.setData({ content: result.content })
      wx.showToast({ title: "已生成处理说明", icon: "success" })
    } catch (error) {
      this.setData({ aiError: (error as Error).message })
    } finally {
      this.setData({ aiLoading: false })
    }
  },

  async submit() {
    if (this.data.content.trim().length < 10) {
      this.setData({ submitError: "处理说明不少于 10 字" })
      return
    }
    if (this.data.photos.length === 0) {
      this.setData({ submitError: "请至少上传 1 张整改后照片" })
      return
    }
    this.setData({ submitting: true, submitError: "" })
    try {
      const ids = await this.uploadResultPhotos()
      await api.submitWorkOrder(this.orderId, this.data.content, ids)
      wx.showToast({ title: "提交成功", icon: "success" })
      this.setData({ photos: [], photoIds: [], content: "" })
      this.load()
    } catch (error) {
      this.setData({ submitError: (error as Error).message })
    } finally {
      this.setData({ submitting: false })
    }
  },

  onProcessContentInput(e: any) {
    // 聚焦输入期间只更新本地，失焦后再自动保存
    this.setData({ processContent: e.detail.value })
  },

  onProcessContentBlur() {
    this.saveProcessRecord()
  },

  chooseProcessPhotos() {
    wx.chooseMedia({
      count: 9 - this.data.processPhotos.length,
      mediaType: ["image"],
      sourceType: ["album", "camera"],
      sizeType: ["compressed"],
      success: async (res) => {
        this.setData({ processSaving: true, processError: "" })
        try {
          const added: ProcessPhoto[] = []
          for (const file of res.tempFiles) {
            const uploaded = await api.upload(file.tempFilePath, "WORKORDER_PROCESS")
            added.push({ id: uploaded.id, url: file.tempFilePath })
          }
          this.setData({ processPhotos: this.data.processPhotos.concat(added), processSaving: false })
          await this.saveProcessRecord()
        } catch (error) {
          this.setData({ processError: (error as Error).message, processSaving: false })
        }
      },
      fail: () => wx.showToast({ title: "未选择照片", icon: "none" }),
    })
  },

  previewProcessPhoto(e: any) {
    const current = e.currentTarget.dataset.src
    const urls = this.data.processPhotos.map((photo) => photo.url)
    wx.previewImage({ current, urls: urls.length ? urls : [current] })
  },

  removeProcessPhoto(e: any) {
    const index = Number(e.currentTarget.dataset.index)
    const processPhotos = this.data.processPhotos.slice()
    processPhotos.splice(index, 1)
    this.setData({ processPhotos })
    this.saveProcessRecord()
  },

  /**
   * 删除本人提交的过程记录（连同其照片），便于撤销误传内容。
   */
  async deleteProcessLog(e: any) {
    const index = Number(e.currentTarget.dataset.index)
    const row = this.data.detail && this.data.detail.logRows[index]
    if (!row) {
      return
    }
    if (!row.id) {
      this.setData({ logError: "记录编号缺失，请重启工单服务后重试" })
      return
    }
    const confirmed = await new Promise<boolean>((resolve) => {
      wx.showModal({
        title: "删除处理记录",
        content: "删除后该条处理记录及其照片不再显示，确认删除？",
        confirmText: "删除",
        success: (res) => resolve(!!res.confirm),
        fail: () => resolve(false),
      })
    })
    if (!confirmed) {
      return
    }
    this.setData({ deletingLogId: row.id, logError: "" })
    try {
      await api.deleteWorkOrderLog(this.orderId, row.id)
      wx.showToast({ title: "已删除", icon: "success" })
      await this.load()
    } catch (error) {
      this.setData({ logError: (error as Error).message })
    } finally {
      this.setData({ deletingLogId: 0 })
    }
  },

  previewLogPhotos(e: any) {
    const index = Number(e.currentTarget.dataset.index)
    const row = this.data.detail && this.data.detail.logRows[index]
    if (!row || !row.images.length) {
      return
    }
    const urls = row.images.map((photo) => photo.accessUrl)
    wx.previewImage({ current: urls[0], urls })
  },
})
