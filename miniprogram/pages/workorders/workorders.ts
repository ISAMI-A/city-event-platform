import { api, WorkOrderItem } from "../../utils/api"

type WorkOrderRow = WorkOrderItem & { statusText: string; statusClass: string; isTodo: boolean }

const STATUS_TEXT: Record<string, string> = {
  PENDING_ACCEPT: "待接单",
  PROCESSING: "处理中",
  PENDING_LEADER_REVIEW: "待组长复核",
  PENDING_ACCEPTANCE: "待验收",
  COMPLETED: "已完成",
}

const FILTERS = [
  { key: "ALL", name: "全部" },
  { key: "TODO", name: "待我处理" },
  { key: "PENDING_ACCEPT", name: "待接单" },
  { key: "PROCESSING", name: "处理中" },
  { key: "PENDING_LEADER_REVIEW", name: "待复核" },
  { key: "PENDING_ACCEPTANCE", name: "待验收" },
  { key: "COMPLETED", name: "已完成" },
]

Page({
  data: {
    list: [] as WorkOrderRow[],
    viewList: [] as WorkOrderRow[],
    filters: FILTERS,
    filterKey: "ALL",
    loading: true,
    error: "",
    isLeader: false,
    todoCount: 0,
    unread: 0,
  },

  onShow() {
    const app = getApp<IAppOption>()
    if (!app.globalData.token) {
      wx.reLaunch({ url: "/pages/login/login" })
      return
    }
    const role = (wx.getStorageSync("role") as string) || app.globalData.role || ""
    const tabBar = (this as any).getTabBar ? (this as any).getTabBar() : null
    if (tabBar) {
      tabBar.sync(role, 0)
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
        // 档案接口异常时按组员视角加载
      }
      const page = isLeader ? await api.teamWorkOrders(1, 50) : await api.myWorkOrders(1, 50)
      const todoStatus = isLeader ? ["PENDING_LEADER_REVIEW", "PENDING_ACCEPT"] : ["PENDING_ACCEPT", "PROCESSING"]
      const list = page.records.map((item) => ({
        ...item,
        statusText: STATUS_TEXT[item.status] || item.status,
        statusClass: item.status === "COMPLETED" ? "tag-green" : "",
        isTodo: todoStatus.indexOf(item.status) >= 0,
      }))
      list.sort((a, b) => {
        if (a.isTodo !== b.isTodo) {
          return a.isTodo ? -1 : 1
        }
        return String(b.assignTime || "").localeCompare(String(a.assignTime || ""))
      })
      const todoCount = list.filter((item) => item.isTodo).length
      this.setData({ list, isLeader, todoCount })
      this.applyFilter()
      this.loadUnread()
    } catch (error) {
      this.setData({ error: (error as Error).message })
    } finally {
      this.setData({ loading: false })
    }
  },

  async loadUnread() {
    try {
      const unread = await api.unreadNotificationCount()
      this.setData({ unread: unread || 0 })
    } catch (e) {
      // 未读数失败不阻塞列表
    }
  },

  onFilter(e: any) {
    this.setData({ filterKey: e.currentTarget.dataset.key })
    this.applyFilter()
  },

  applyFilter() {
    const key = this.data.filterKey
    let viewList = this.data.list
    if (key === "TODO") {
      viewList = this.data.list.filter((item) => item.isTodo)
    } else if (key !== "ALL") {
      viewList = this.data.list.filter((item) => item.status === key)
    }
    this.setData({ viewList })
  },

  goDetail(e: any) {
    wx.navigateTo({ url: `/pages/workorder-detail/workorder-detail?id=${e.currentTarget.dataset.id}` })
  },

  goTeam() {
    wx.navigateTo({ url: "/pages/team/team" })
  },

  goNotifications() {
    wx.navigateTo({ url: "/pages/notifications/notifications" })
  },
})
