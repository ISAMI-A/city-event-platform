import { api, LoginResult } from "../../utils/api"

function getWxCode(): Promise<string> {
  return new Promise((resolve, reject) => {
    wx.login({
      success: (res) => {
        if (res.code) {
          resolve(res.code)
        } else {
          reject(new Error("获取微信登录凭证失败"))
        }
      },
      fail: () => reject(new Error("微信登录失败，请重试")),
    })
  })
}

Page({
  data: {
    mode: "wx" as "wx" | "account" | "register",
    bindPanel: false,
    bindMode: "" as "" | "worker" | "resident",
    bindTicket: "",
    username: "",
    password: "",
    nickname: "",
    avatarTempPath: "",
    regUsername: "",
    regPassword: "",
    regConfirm: "",
    regNickname: "",
    regPhone: "",
    regLoading: false,
    loading: false,
    wxLoading: false,
    error: "",
  },

  toggleMode() {
    this.setData({
      mode: this.data.mode === "wx" ? "account" : "wx",
      error: "",
    })
  },

  onOpenRegister() {
    this.setData({ mode: "register", error: "" })
  },

  onBackLogin() {
    this.setData({ mode: "account", error: "" })
  },

  onRegUsernameInput(e: any) {
    this.setData({ regUsername: e.detail.value })
  },

  onRegPasswordInput(e: any) {
    this.setData({ regPassword: e.detail.value })
  },

  onRegConfirmInput(e: any) {
    this.setData({ regConfirm: e.detail.value })
  },

  onRegNicknameInput(e: any) {
    this.setData({ regNickname: e.detail.value })
  },

  onRegPhoneInput(e: any) {
    this.setData({ regPhone: e.detail.value })
  },

  async onRegister() {
    const username = this.data.regUsername.trim()
    const password = this.data.regPassword
    const nickname = this.data.regNickname.trim()
    const phone = this.data.regPhone.trim()
    if (!/^[a-zA-Z][a-zA-Z0-9_]{3,31}$/.test(username)) {
      this.setData({ error: "用户名需以字母开头，4-32 位字母、数字或下划线" })
      return
    }
    if (password.length < 8 || password.length > 32) {
      this.setData({ error: "密码长度需在 8-32 之间" })
      return
    }
    if (password !== this.data.regConfirm) {
      this.setData({ error: "两次输入的密码不一致" })
      return
    }
    if (nickname.length < 2 || nickname.length > 20) {
      this.setData({ error: "昵称长度需在 2-20 之间" })
      return
    }
    if (!/^1[3-9]\d{9}$/.test(phone)) {
      this.setData({ error: "请输入正确的 11 位手机号" })
      return
    }
    this.setData({ regLoading: true, error: "" })
    try {
      const data = await api.register({ username, password, nickname, phone })
      this.saveSession(data)
      wx.showToast({ title: "注册成功", icon: "success" })
      this.redirectByRole(data.activeRole || "RESIDENT")
    } catch (error) {
      this.setData({ error: (error as Error).message })
    } finally {
      this.setData({ regLoading: false })
    }
  },

  onUsernameInput(e: any) {
    this.setData({ username: e.detail.value })
  },

  onPasswordInput(e: any) {
    this.setData({ password: e.detail.value })
  },

  onNicknameInput(e: any) {
    this.setData({ nickname: e.detail.value })
  },

  /**
   * 微信昵称输入框在失焦 / 合规检测回调里才把最终昵称写回 data。
   * 失焦事件可能不带 value，不能盲目回写空串，否则会清掉已选中的微信昵称。
   */
  onNicknameBlur(e: any) {
    const value = e.detail && e.detail.value
    if (typeof value === "string" && value.trim()) {
      this.setData({ nickname: value.trim() })
    }
  },

  onNicknameReview(e: any) {
    const detail = e.detail || {}
    const name = detail.nickname || detail.value || ""
    if (name) {
      this.setData({ nickname: name })
    }
  },

  onChooseAvatar(e: any) {
    this.setData({ avatarTempPath: e.detail.avatarUrl })
  },

  async onWxLogin() {
    this.setData({ wxLoading: true, error: "" })
    try {
      const code = await getWxCode()
      const data = await api.wxLogin(code)
      if (data.needBind) {
        this.setData({
          bindPanel: true,
          bindMode: "",
          bindTicket: data.bindTicket || "",
        })
        return
      }
      this.handleLoginSuccess(data)
    } catch (error) {
      this.setData({ error: (error as Error).message })
    } finally {
      this.setData({ wxLoading: false })
    }
  },

  onChooseWorker() {
    this.setData({ bindMode: "worker", error: "" })
  },

  onChooseResident() {
    this.setData({ bindMode: "resident", error: "", nickname: "", avatarTempPath: "" })
  },

  onBackChoose() {
    this.setData({ bindMode: "", error: "" })
  },

  onCancelBind() {
    this.setData({
      bindPanel: false,
      bindMode: "",
      bindTicket: "",
      nickname: "",
      avatarTempPath: "",
      error: "",
    })
  },

  async onBindWorker() {
    const { bindTicket, username, password } = this.data
    if (!username || !password) {
      this.setData({ error: "请输入账号和密码" })
      return
    }
    this.setData({ loading: true, error: "" })
    try {
      const data = await api.wxBind(bindTicket, username, password)
      this.handleLoginSuccess(data)
    } catch (error) {
      this.setData({ error: (error as Error).message })
    } finally {
      this.setData({ loading: false })
    }
  },

  async onRegisterResident() {
    this.setData({ loading: true, error: "" })
    try {
      const data = await api.wxRegister(this.data.bindTicket, this.data.nickname.trim())
      this.saveSession(data)
      if (this.data.avatarTempPath) {
        try {
          const uploaded = await api.upload(this.data.avatarTempPath, "USER_AVATAR")
          await api.updateProfile(undefined, uploaded.accessUrl)
        } catch (e) {
          // 头像上传失败不阻断登录，用户可在后续资料中补充
        }
      }
      this.redirectByRole(data.activeRole || "")
    } catch (error) {
      this.setData({ error: (error as Error).message })
    } finally {
      this.setData({ loading: false })
    }
  },

  async onLogin() {
    const { username, password } = this.data
    if (!username || !password) {
      this.setData({ error: "请输入账号和密码" })
      return
    }
    this.setData({ loading: true, error: "" })
    try {
      const data = await api.login(username, password, "miniprogram")
      this.handleLoginSuccess(data)
    } catch (error) {
      this.setData({ error: (error as Error).message })
    } finally {
      this.setData({ loading: false })
    }
  },

  saveSession(data: LoginResult) {
    const app = getApp<IAppOption>()
    app.globalData.token = data.token || ""
    app.globalData.role = data.activeRole || ""
    app.globalData.nickname = data.nickname || ""
    wx.setStorageSync("token", data.token)
    wx.setStorageSync("role", data.activeRole)
    wx.setStorageSync("nickname", data.nickname || "")
  },

  redirectByRole(activeRole: string) {
    wx.reLaunch({
      url: activeRole === "WORKER"
        ? "/pages/workorders/workorders"
        : "/pages/events/events",
    })
  },

  handleLoginSuccess(data: LoginResult) {
    this.saveSession(data)
    if (data.activeRole === "ADMIN") {
      this.setData({ error: "管理员请使用 Web 管理后台" })
      return
    }
    this.redirectByRole(data.activeRole || "")
  },
})
