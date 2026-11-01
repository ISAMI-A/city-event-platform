import { api, EventItem, RecruitmentItem } from "../../utils/api"
import { localImage } from "../../utils/image"

type EventRow = EventItem & { statusText: string; statusClass: string }
type RecruitmentRow = RecruitmentItem & { cover: string }

const STATUS_TEXT: Record<string, string> = {
  PENDING_REVIEW: "待审核",
  REJECTED: "已驳回",
  CANCELLED: "已撤销",
  DISPATCHED: "已派单",
  PROCESSING: "处理中",
  PENDING_ACCEPTANCE: "待验收",
  CLOSED: "已关闭",
}

const FILTERS = [
  { key: "ALL", name: "全部" },
  { key: "PENDING_REVIEW", name: "待审核" },
  { key: "PROCESSING", name: "处理中" },
  { key: "CLOSED", name: "已完成" },
  { key: "REJECTED", name: "已驳回" },
  { key: "CANCELLED", name: "已撤销" },
]

function matchFilter(status: string, key: string) {
  if (key === "ALL") {
    return true
  }
  if (key === "PROCESSING") {
    return ["DISPATCHED", "PROCESSING", "PENDING_ACCEPTANCE"].indexOf(status) >= 0
  }
  return status === key
}

function absoluteUrl(url: string) {
  if (!url || url.startsWith("http")) {
    return url
  }
  return getApp<IAppOption>().globalData.baseUrl + url
}

Page({
  data: {
    list: [] as EventRow[],
    viewList: [] as EventRow[],
    filters: FILTERS,
    filterKey: "ALL",
    loading: true,
    error: "",
    recruitments: [] as RecruitmentRow[],
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
    if (role === "RESIDENT") {
      this.loadRecruitments()
    }
  },

  onPullDownRefresh() {
    this.load().finally(() => wx.stopPullDownRefresh())
  },

  async load() {
    this.setData({ loading: true, error: "" })
    try {
      const page = await api.myEvents(1, 50)
      const list = page.records.map((item) => ({
        ...item,
        statusText: STATUS_TEXT[item.status] || item.status,
        statusClass: item.status === "CLOSED" ? "tag-green" : item.status === "REJECTED" ? "tag-red" : "",
      }))
      this.setData({ list })
      this.applyFilter()
    } catch (error) {
      this.setData({ error: (error as Error).message })
    } finally {
      this.setData({ loading: false })
    }
  },

  onFilter(e: any) {
    this.setData({ filterKey: e.currentTarget.dataset.key })
    this.applyFilter()
  },

  applyFilter() {
    const key = this.data.filterKey
    this.setData({ viewList: this.data.list.filter((item) => matchFilter(item.status, key)) })
  },

  goDetail(e: any) {
    wx.navigateTo({ url: `/pages/detail/detail?id=${e.currentTarget.dataset.id}` })
  },

  goReport() {
    wx.navigateTo({ url: "/pages/report/report" })
  },

  /**
   * 居民端首页招募轮播：只展示招募中且未过期的岗位。
   */
  async loadRecruitments() {
    try {
      const items = await api.openRecruitments()
      const list = await Promise.all(
        (items || []).slice(0, 5).map(async (item) => ({
          ...item,
          cover: item.coverUrl ? await localImage(absoluteUrl(item.coverUrl)) : "",
        })),
      )
      this.setData({ recruitments: list })
    } catch (e) {
      this.setData({ recruitments: [] })
    }
  },

  goRecruitment(e: any) {
    wx.navigateTo({ url: `/pages/recruitment-detail/recruitment-detail?id=${e.currentTarget.dataset.id}` })
  },
})
