import { api, NotificationItem, WorkerItem } from "../../utils/api"
import { splitNotice } from "../../utils/util"

type NoticeRow = NotificationItem & {
  timeText: string
  typeText: string
  canForward: boolean
  readClass: string
  bodyText: string
  receiptText: string
}

const TYPE_TAG: Record<string, string> = {
  WORK_ORDER_ASSIGNED: "工单派单",
  RATING_REMIND: "管理员提醒",
  LEADER_FORWARD: "组长转达",
  TEAM_NOTICE: "注意事项",
  TEAM_NOTICE_SENT: "我发布的",
}

Page({
  data: {
    list: [] as NoticeRow[],
    loading: true,
    error: "",
    unread: 0,
    isLeader: false,
    members: [] as WorkerItem[],
    noticePanel: false,
    noticeText: "",
    saving: false,
  },

  onShow() {
    const app = getApp<IAppOption>()
    if (!app.globalData.token) {
      wx.reLaunch({ url: "/pages/login/login" })
      return
    }
    this.load()
  },

  onPullDownRefresh() {
    this.load().finally(() => wx.stopPullDownRefresh())
  },

  async load() {
    this.setData({ loading: true, error: "" })
    try {
      let isLeader = false
      try {
        const profile = await api.workerProfile()
        isLeader = profile.workerType === "LEADER"
      } catch (e) {
        // 非处理人员或档案异常，按普通身份展示
      }
      const raw = await api.getNotifications()
      const list = raw.map((item) => {
        const parts = splitNotice(item.content, item.bizType)
        return {
          ...item,
          timeText: String(item.createTime || "").replace("T", " "),
          typeText: TYPE_TAG[item.bizType] || "系统通知",
          canForward: isLeader && item.bizType === "RATING_REMIND",
          readClass: item.isRead ? "" : "unread",
          bodyText: parts.body,
          receiptText: parts.receipt,
        }
      })
      const unread = list.filter((item) => !item.isRead).length
      this.setData({ list, unread, isLeader })
      if (isLeader) {
        try {
          const members = await api.myTeam()
          this.setData({ members })
        } catch (e) {
          // 组员列表仅用于转达，失败不阻塞
        }
      }
    } catch (error) {
      this.setData({ error: (error as Error).message })
    } finally {
      this.setData({ loading: false })
    }
  },

  openDetail(e: any) {
    const id = Number(e.currentTarget.dataset.id)
    wx.navigateTo({ url: `/pages/notification-detail/notification-detail?id=${id}` })
  },

  onForward(e: any) {
    const workOrderId = Number(e.currentTarget.dataset.wo)
    const members = this.data.members
    if (!members.length) {
      wx.showToast({ title: "本组暂无在岗组员", icon: "none" })
      return
    }
    const names = members.map((item) => item.nickname || item.username || "组员")
    wx.showActionSheet({
      itemList: names,
      success: (res) => {
        const member = members[res.tapIndex]
        if (!member) {
          return
        }
        wx.showModal({
          title: "通知组员",
          editable: true,
          placeholderText: "补充要求，例如：请今天内联系居民确认整改效果",
          success: async (modal) => {
            if (!modal.confirm) {
              return
            }
            try {
              await api.forwardNotice(workOrderId, member.id, modal.content || "请尽快核实处理并反馈")
              wx.showToast({ title: "已通知组员", icon: "success" })
            } catch (error) {
              wx.showToast({ title: (error as Error).message, icon: "none" })
            }
          },
        })
      },
    })
  },

  openNotice() {
    this.setData({ noticePanel: true, noticeText: "" })
  },

  closeNotice() {
    this.setData({ noticePanel: false })
  },

  noop() {
    // 阻止弹层内部点击冒泡到遮罩
  },

  onNoticeInput(e: any) {
    this.setData({ noticeText: e.detail.value })
  },

  async submitNotice() {
    const content = (this.data.noticeText || "").trim()
    if (content.length < 5) {
      wx.showToast({ title: "内容不少于 5 个字", icon: "none" })
      return
    }
    this.setData({ saving: true })
    try {
      const count = await api.publishTeamNotice(content)
      wx.showToast({ title: `已发送给 ${count} 名组员`, icon: "success" })
      this.setData({ noticePanel: false, noticeText: "" })
      this.load()
    } catch (error) {
      wx.showToast({ title: (error as Error).message, icon: "none" })
    } finally {
      this.setData({ saving: false })
    }
  },
})
