import { api } from "../../utils/api"
import { localImage } from "../../utils/image"

const CATEGORIES = [
  { code: "ROAD_FACILITY", name: "道路设施" },
  { code: "PUBLIC_LIGHTING", name: "公共照明" },
  { code: "ENVIRONMENT_SANITATION", name: "环境卫生" },
  { code: "PUBLIC_FACILITY", name: "公共设施" },
  { code: "OTHER", name: "其他" },
]

/**
 * 福州市六城区经纬度外包框（GCJ-02），与后端 event-service 的 FuzhouArea 保持一致。
 */
function inFuzhou(longitude: number, latitude: number) {
  return longitude >= 118.35 && longitude <= 119.95 && latitude >= 25.75 && latitude <= 26.35
}

Page({
  data: {
    loading: false,
    isResubmit: false,
    eventId: 0,
    description: "",
    address: "",
    longitude: 0,
    latitude: 0,
    areaCode: "",
    photos: [] as string[],
    originalPhotos: [] as string[],
    categoryIndex: 0,
    categoryNames: CATEGORIES.map((item) => item.name),
    priorityIndex: 1,
    priorityNames: ["低", "中", "高", "紧急"],
    priorities: ["LOW", "MEDIUM", "HIGH", "URGENT"],
    ai: null as null | {
      callId: number
      categoryCode: string
      description: string
      summary: string
      priority: string
      suggestion: string
      reasoning: string
      confidence: number
      degraded: boolean
      degradeReason: string
    },
    aiSnapshot: null as null | {
      categoryCode: string
      priority: string
      summary: string
      suggestion: string
      confidence: number
    },
    uploadedMap: {} as Record<string, { id: number; accessUrl: string }>,
    aiLoading: false,
    aiError: "",
    submitting: false,
    error: "",
    locationDenied: false,
  },

  onLoad(options: Record<string, string>) {
    const app = getApp<IAppOption>()
    if (!app.globalData.token) {
      wx.reLaunch({ url: "/pages/login/login" })
      return
    }
    if (options.mode === "resubmit" && options.id) {
      const eventId = Number(options.id)
      this.setData({ isResubmit: true, eventId })
      wx.setNavigationBarTitle({ title: "修改并重新提交" })
      this.loadEvent(eventId)
    }
  },

  async loadEvent(id: number) {
    this.setData({ loading: true, error: "" })
    try {
      const detail = await api.eventDetail(id)
      const baseUrl = getApp<IAppOption>().globalData.baseUrl
      const categoryIndex = CATEGORIES.findIndex((item) => item.code === detail.categoryCode)
      const priorityIndex = this.data.priorities.indexOf(detail.priority)
      const originalPhotos = await Promise.all(
        detail.attachments.map((item) =>
          localImage(item.accessUrl.startsWith("http") ? item.accessUrl : baseUrl + item.accessUrl),
        ),
      )
      this.setData({
        description: detail.description || "",
        address: detail.address || "",
        longitude: Number(detail.longitude) || 0,
        latitude: Number(detail.latitude) || 0,
        areaCode: detail.areaCode || "",
        categoryIndex: categoryIndex >= 0 ? categoryIndex : 0,
        priorityIndex: priorityIndex >= 0 ? priorityIndex : 1,
        originalPhotos,
        aiSnapshot: detail.aiSnapshot || null,
      })
    } catch (error) {
      this.setData({ error: (error as Error).message })
    } finally {
      this.setData({ loading: false })
    }
  },

  onDescriptionInput(e: any) {
    this.setData({ description: e.detail.value })
  },

  onCategoryChange(e: any) {
    this.setData({ categoryIndex: Number(e.detail.value) })
  },

  onPriorityChange(e: any) {
    this.setData({ priorityIndex: Number(e.detail.value) })
  },

  chooseLocation() {
    wx.chooseLocation({
      success: (res) => {
        // 仅开放福州市六城区：与后端 FuzhouArea 校验保持一致
        if (!inFuzhou(res.longitude, res.latitude)) {
          this.setData({
            error: "仅支持福州市六城区（鼓楼、台江、仓山、马尾、晋安、长乐）范围内上报",
          })
          return
        }
        // name 可能是具体 POI（如"福建农林大学"），address 可能是省市级，两者拼接展示
        const parts = [res.name, res.address].filter(
          (item, index, arr) => item && arr.indexOf(item) === index,
        )
        this.setData({
          address: parts.join(" "),
          longitude: res.longitude,
          latitude: res.latitude,
          locationDenied: false,
          error: "",
        })
      },
      fail: () => {
        this.setData({
          locationDenied: true,
          error: "未获取到位置权限，请在设置中允许位置信息后重试",
        })
      },
    })
  },

  choosePhotos() {
    wx.chooseMedia({
      count: 9 - this.data.photos.length,
      mediaType: ["image"],
      sourceType: ["album", "camera"],
      // 强制压缩为 JPG，避免 iOS 相册原图（HEIC）与后端 jpg/png 校验不匹配
      sizeType: ["compressed"],
      success: (res) => {
        const paths = res.tempFiles.map((file) => file.tempFilePath)
        this.setData({ photos: this.data.photos.concat(paths) })
      },
      fail: () => {
        wx.showToast({ title: "未选择照片", icon: "none" })
      },
    })
  },

  removePhoto(e: any) {
    const index = Number(e.currentTarget.dataset.index)
    const photos = this.data.photos.slice()
    photos.splice(index, 1)
    this.setData({ photos })
  },

  async analyze() {
    if (this.data.isResubmit) {
      return
    }
    const description = this.data.description.trim()
    if (!description && this.data.photos.length === 0) {
      this.setData({ error: "请填写事件描述或上传照片后再使用 AI 分析" })
      return
    }
    this.setData({ aiLoading: true, aiError: "", error: "" })
    let uploaded: { id: number; accessUrl: string }[] = []
    try {
      uploaded = await this.ensureUploaded()
    } catch (error) {
      this.setData({
        aiError: "照片上传失败：" + ((error as Error).message || "请检查网络后重试"),
        aiLoading: false,
      })
      return
    }
    try {
      const imageUrls = uploaded.map((item) => item.accessUrl)
      const ai = await api.analyze(description, this.data.address, imageUrls)
      const index = CATEGORIES.findIndex((item) => item.code === ai.categoryCode)
      const priorityIndex = this.data.priorities.indexOf(ai.priority)
      this.setData({
        ai,
        description: ai.description ? ai.description : this.data.description,
        categoryIndex: index >= 0 ? index : 0,
        priorityIndex: priorityIndex >= 0 ? priorityIndex : 1,
      })
    } catch (error) {
      this.setData({
        aiError: "AI 分析暂不可用，请手动填写描述、类别和优先级后继续上报",
      })
    } finally {
      this.setData({ aiLoading: false })
    }
  },

  async ensureUploaded() {
    const map = this.data.uploadedMap
    const result: { id: number; accessUrl: string }[] = []
    for (const photo of this.data.photos) {
      if (!map[photo]) {
        map[photo] = await api.upload(photo, "EVENT_REPORT")
      }
      result.push(map[photo])
    }
    this.setData({ uploadedMap: map })
    return result
  },

  async submit() {
    const {
      description,
      photos,
      longitude,
      latitude,
      address,
      categoryIndex,
      priorityIndex,
      isResubmit,
      eventId,
    } = this.data
    if (description.trim().length < 10) {
      this.setData({ error: "事件描述不少于 10 字" })
      return
    }
    if (!address || !longitude || !latitude) {
      this.setData({ error: "请先在地图上选择事件位置" })
      return
    }
    if (!inFuzhou(longitude, latitude)) {
      this.setData({ error: "仅支持福州市六城区（鼓楼、台江、仓山、马尾、晋安、长乐）范围内上报" })
      return
    }
    if (!isResubmit && photos.length === 0) {
      this.setData({ error: "请至少上传 1 张照片" })
      return
    }
    this.setData({ submitting: true, error: "" })
    try {
      const uploaded = photos.length > 0 ? await this.ensureUploaded() : []
      const attachmentIds = uploaded.map((item) => item.id)
      const data = {
        description,
        categoryCode: CATEGORIES[categoryIndex].code,
        priority: this.data.priorities[priorityIndex],
        longitude,
        latitude,
        address,
        attachmentIds,
      }
      if (isResubmit) {
        await api.resubmitEvent(eventId, { ...data, areaCode: this.data.areaCode })
        wx.showToast({ title: "重新提交成功", icon: "success" })
        setTimeout(() => wx.switchTab({ url: "/pages/events/events" }), 800)
      } else {
        await api.createEvent({
          ...data,
          aiCallId: this.data.ai ? this.data.ai.callId : undefined,
        })
        wx.showToast({ title: "上报成功", icon: "success" })
        setTimeout(() => wx.reLaunch({ url: "/pages/events/events" }), 800)
      }
    } catch (error) {
      this.setData({ error: (error as Error).message })
    } finally {
      this.setData({ submitting: false })
    }
  },
})
