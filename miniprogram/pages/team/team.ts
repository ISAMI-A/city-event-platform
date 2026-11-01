import { api } from "../../utils/api"

interface MemberRow {
  id: number
  nickname: string
  workZone: string
  statusText: string
  activeCount: number
  completedCount: number
  avgRating: number
  avgHours: number
  returnCount: number
  score: number
}

Page({
  data: {
    loading: true,
    error: "",
    workZone: "",
    teamSize: 0,
    members: [] as MemberRow[],
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
      const profile = await api.workerProfile()
      if (profile.workerType !== "LEADER") {
        wx.showToast({ title: "仅组长可查看团队", icon: "none" })
        setTimeout(() => wx.navigateBack(), 800)
        return
      }
      const team = await api.myTeam()
      const [stats, loads] = await Promise.all([
        api.teamStats(),
        team.length
          ? api.workOrderLoads(team.map((item) => item.id))
          : Promise.resolve([] as { workerId: number; activeCount: number }[]),
      ])
      const statMap: Record<number, (typeof stats)[number]> = {}
      stats.forEach((item) => {
        statMap[item.workerId] = item
      })
      const loadMap: Record<number, number> = {}
      loads.forEach((item) => {
        loadMap[item.workerId] = item.activeCount
      })
      const members = team.map((item) => {
        const stat = statMap[item.id]
        return {
          id: item.id,
          nickname: item.nickname,
          workZone: item.workZone || "-",
          statusText: item.status === "ON_DUTY" ? "在岗" : item.status === "OFF_DUTY" ? "离岗" : "-",
          activeCount: loadMap[item.id] || 0,
          completedCount: stat ? stat.completedCount : 0,
          avgRating: stat ? stat.avgRating : 0,
          avgHours: stat ? stat.avgHours : 0,
          returnCount: stat ? stat.returnCount : 0,
          score: stat ? stat.score : 0,
        }
      })
      this.setData({
        workZone: profile.workZone || profile.areaName || "-",
        teamSize: members.length,
        members,
      })
    } catch (error) {
      this.setData({ error: (error as Error).message })
    } finally {
      this.setData({ loading: false })
    }
  },
})
