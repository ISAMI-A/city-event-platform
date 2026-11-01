import { api, NotificationItem, WorkerItem } from "../../utils/api"
import { splitNotice } from "../../utils/util"

const TYPE_TAG: Record<string, string> = {
  WORK_ORDER_ASSIGNED: "工单派单",
  RATING_REMIND: "管理员提醒",
  LEADER_FORWARD: "组长转达",
  TEAM_NOTICE: "注意事项",
  TEAM_NOTICE_SENT: "我发布的",
}

Page({
  data: {
    id: 0,
    item: null as NotificationItem | null,
    typeText: "",
    timeText: "",
    bodyText: "",
    receiptText: "",
    canForward: false,
    members: [] as WorkerItem[],
  },

  onLoad(options: any) {
    const id = Number(options.id || 0)
    this.setData({ id })
    this.load()
  },

  async load() {
    try {
      const item = await api.getNotification(this.data.id)
      let isLeader = false
      try {
        const profile = await api.workerProfile()
        isLeader = profile.workerType === "LEADER"
      } catch (e) {
        // 非处理人员，忽略
      }
      const canForward = isLeader && item.bizType === "RATING_REMIND"
      const parts = splitNotice(item.content, item.bizType)
      this.setData({
        item,
        canForward,
        typeText: TYPE_TAG[item.bizType] || "系统通知",
        timeText: String(item.createTime || "").replace("T", " "),
        bodyText: parts.body,
        receiptText: parts.receipt,
      })
      if (canForward) {
        try {
          const members = await api.myTeam()
          this.setData({ members })
        } catch (e) {
          // 组员列表加载失败不影响阅读
        }
      }
      if (!item.isRead) {
        try {
          await api.readNotification(item.id)
        } catch (e) {
          // 已读标记失败不打断阅读
        }
      }
    } catch (error) {
      wx.showToast({ title: (error as Error).message, icon: "none" })
    }
  },

  copyContent() {
    const text = this.data.bodyText
    if (!text) {
      return
    }
    wx.setClipboardData({
      data: text,
      success: () => wx.showToast({ title: "内容已复制", icon: "success" }),
    })
  },

  onForward() {
    const item = this.data.item
    if (!item) {
      return
    }
    const members = this.data.members
    if (!members.length) {
      wx.showToast({ title: "本组暂无在岗组员", icon: "none" })
      return
    }
    wx.showActionSheet({
      itemList: members.map((member) => member.nickname || "组员"),
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
              await api.forwardNotice(item.bizId, member.id, modal.content || "请尽快核实处理并反馈")
              wx.showToast({ title: "已通知组员", icon: "success" })
            } catch (error) {
              wx.showToast({ title: (error as Error).message, icon: "none" })
            }
          },
        })
      },
    })
  },
})
