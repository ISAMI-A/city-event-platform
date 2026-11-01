import { api, UserProfile, WorkerProfileInfo } from "../../utils/api"
import { localImage } from "../../utils/image"

const ROLE_TEXT: Record<string, string> = {
  RESIDENT: "居民",
  WORKER: "处理人员",
  ADMIN: "管理员",
}

function absoluteUrl(url?: string) {
  if (!url) {
    return ""
  }
  if (url.startsWith("http")) {
    return url
  }
  return getApp<IAppOption>().globalData.baseUrl + url
}

function dutyText(profile: WorkerProfileInfo) {
  if (profile.status === "ON_DUTY") {
    return "在岗"
  }
  if (profile.status === "OFF_DUTY") {
    return "离岗"
  }
  if (typeof profile.onDuty === "boolean") {
    return profile.onDuty ? "在岗" : "离岗"
  }
  return profile.status || "-"
}

function accountText(status?: string) {
  if (!status) {
    return "-"
  }
  if (status === "ENABLED") {
    return "正常"
  }
  if (status === "DISABLED") {
    return "已停用"
  }
  return status
}

Page({
  data: {
    loading: true,
    error: "",
    me: null as UserProfile | null,
    role: "",
    roleText: "居民",
    avatarPreview: "",
    avatarText: "我",
    phone: "",
    email: "",
    emailVerified: false,
    hasPassword: false,
    wxBound: false,
    worker: null as null | {
      typeText: string
      deptName: string
      workZone: string
      leaderName: string
      teamSize: number
      dutyText: string
      accountText: string
    },
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
      tabBar.sync(role, 1)
    }
    this.load()
  },

  async load() {
    this.setData({ loading: true, error: "" })
    try {
      const me = await api.getMe()
      const role = me.activeRole || (wx.getStorageSync("role") as string) || ""
      const tabBar = (this as any).getTabBar ? (this as any).getTabBar() : null
      if (tabBar) {
        tabBar.sync(role, 1)
      }
      const nickname = me.nickname || ""
      const avatarPreview = await localImage(absoluteUrl(me.avatarUrl))
      this.setData({
        me,
        role,
        roleText: ROLE_TEXT[role] || "居民",
        avatarPreview,
        avatarText: (nickname || me.username || "我").slice(0, 1),
        phone: me.phone || "",
        email: me.email || "",
        emailVerified: me.emailVerified === true,
        hasPassword: me.hasPassword === true,
        wxBound: me.wxBound === true,
      })
      if (role === "WORKER") {
        this.loadWorkerProfile()
      } else {
        this.setData({ worker: null })
      }
    } catch (error) {
      this.setData({ error: (error as Error).message })
    } finally {
      this.setData({ loading: false })
    }
  },

  async loadWorkerProfile() {
    try {
      const profile = await api.workerProfile()
      this.setData({
        worker: {
          typeText: profile.workerType === "LEADER" ? "组长" : "组员",
          deptName: profile.deptName || profile.deptCode || "-",
          workZone: profile.workZone || profile.areaName || profile.areaCode || "-",
          leaderName: profile.leaderName || (profile.workerType === "LEADER" ? "本人" : "-"),
          teamSize: profile.teamSize || 0,
          dutyText: dutyText(profile),
          accountText: accountText(profile.accountStatus),
        },
      })
    } catch (e) {
      // 居民或档案接口暂不可用时忽略，不影响“我的”页其他信息
      this.setData({ worker: null })
    }
  },

  editProfile() {
    wx.navigateTo({ url: "/pages/profile-edit/profile-edit" })
  },

  logout() {
    wx.showModal({
      title: "退出登录",
      content: "确定要退出当前账号吗？",
      success: async (res) => {
        if (!res.confirm) {
          return
        }
        try {
          await api.logout()
        } catch (e) {
          // 服务端注销失败也继续清理本地登录态
        }
        const app = getApp<IAppOption>()
        app.globalData.token = ""
        app.globalData.role = ""
        app.globalData.nickname = ""
        wx.removeStorageSync("token")
        wx.removeStorageSync("role")
        wx.removeStorageSync("nickname")
        wx.reLaunch({ url: "/pages/login/login" })
      },
    })
  },
})
