import { api, UserProfile } from "../../utils/api"
import { localImage } from "../../utils/image"

function absoluteUrl(url?: string) {
  if (!url) {
    return ""
  }
  if (url.startsWith("http")) {
    return url
  }
  return getApp<IAppOption>().globalData.baseUrl + url
}

function getWxCode(): Promise<string> {
  return new Promise((resolve, reject) => {
    wx.login({
      success: (res) => {
        if (res.code) {
          resolve(res.code)
        } else {
          reject(new Error("获取微信凭证失败，请重试"))
        }
      },
      fail: () => reject(new Error("获取微信凭证失败，请重试")),
    })
  })
}

Page({
  data: {
    loading: true,
    loadError: "",
    avatarError: "",
    nicknameError: "",
    usernameError: "",
    phoneError: "",
    emailError: "",
    passwordError: "",
    wxError: "",
    me: null as UserProfile | null,
    nicknameInput: "",
    avatarPreview: "",
    avatarRaw: "",
    avatarDraftRaw: "",
    avatarText: "我",
    uploading: false,
    savingProfile: false,
    username: "",
    usernameInput: "",
    usernamePassword: "",
    usernameSaving: false,
    phone: "",
    email: "",
    emailVerified: false,
    hasPassword: false,
    wxBound: false,
    panel: "",
    phoneInput: "",
    phonePassword: "",
    phoneSaving: false,
    emailInput: "",
    emailCode: "",
    emailPassword: "",
    emailCodeSending: false,
    emailCountdown: 0,
    emailSaving: false,
    oldPassword: "",
    newPassword: "",
    confirmPassword: "",
    passwordSaving: false,
    wxLoading: false,
  },

  emailTimer: 0,

  onShow() {
    const app = getApp<IAppOption>()
    if (!app.globalData.token) {
      wx.reLaunch({ url: "/pages/login/login" })
      return
    }
    this.load()
  },

  onUnload() {
    this.stopEmailCountdown()
  },

  async load() {
    this.setData({ loading: true, loadError: "" })
    try {
      const me = await api.getMe()
      const avatarPreview = await localImage(absoluteUrl(me.avatarUrl))
      const nickname = me.nickname || ""
      this.setData({
        me,
        nicknameInput: this.data.nicknameInput || nickname,
        avatarPreview: this.data.avatarDraftRaw ? this.data.avatarPreview : avatarPreview,
        avatarRaw: me.avatarUrl || "",
        avatarText: (nickname || me.username || "我").slice(0, 1),
        username: me.username || "",
        phone: me.phone || "",
        email: me.email || "",
        emailVerified: me.emailVerified === true,
        hasPassword: me.hasPassword === true,
        wxBound: me.wxBound === true,
      })
    } catch (error) {
      this.setData({ loadError: (error as Error).message })
    } finally {
      this.setData({ loading: false })
    }
  },

  chooseAvatar() {
    wx.chooseMedia({
      count: 1,
      mediaType: ["image"],
      sizeType: ["compressed"],
      sourceType: ["album", "camera"],
      success: async (res) => {
        const tempPath = res.tempFiles[0] ? res.tempFiles[0].tempFilePath : ""
        if (!tempPath) {
          return
        }
        this.setData({ uploading: true, avatarError: "" })
        try {
          const uploaded = await api.upload(tempPath, "USER_AVATAR")
          // 预览用本地临时文件，避免真机加载 http 图片被拦截
          this.setData({ avatarPreview: tempPath, avatarDraftRaw: uploaded.accessUrl })
        } catch (error) {
          this.setData({ avatarError: (error as Error).message })
        } finally {
          this.setData({ uploading: false })
        }
      },
    })
  },

  onNicknameInput(e: any) {
    this.setData({ nicknameInput: e.detail.value, nicknameError: "" })
  },

  async saveProfile() {
    const nickname = this.data.nicknameInput.trim()
    if (!nickname) {
      this.setData({ nicknameError: "昵称不能为空" })
      return
    }
    const me = this.data.me
    if (!this.data.avatarDraftRaw && me && nickname === (me.nickname || "")) {
      wx.showToast({ title: "资料未修改", icon: "none" })
      return
    }
    this.setData({ savingProfile: true, nicknameError: "", avatarError: "" })
    try {
      await api.updateProfile(nickname, this.data.avatarDraftRaw || this.data.avatarRaw || undefined)
      this.setData({ avatarDraftRaw: "" })
      wx.showToast({ title: "资料已更新", icon: "success" })
      await this.load()
    } catch (error) {
      this.setData({ nicknameError: (error as Error).message })
    } finally {
      this.setData({ savingProfile: false })
    }
  },

  /**
   * 展开 / 收起某一块的修改区域，同一时间只展开一块。
   */
  togglePanel(e: any) {
    const name = (e.currentTarget.dataset.name || "") as string
    if (this.data.panel === name) {
      this.setData({ panel: "", usernameError: "", phoneError: "", emailError: "", passwordError: "" })
      return
    }
    this.setData({
      panel: name,
      usernameError: "",
      phoneError: "",
      emailError: "",
      passwordError: "",
      usernameInput: "",
      usernamePassword: "",
      phoneInput: "",
      phonePassword: "",
      emailInput: "",
      emailCode: "",
      emailPassword: "",
      oldPassword: "",
      newPassword: "",
      confirmPassword: "",
    })
  },

  onUsernameInput(e: any) {
    this.setData({ usernameInput: e.detail.value, usernameError: "" })
  },

  onUsernamePasswordInput(e: any) {
    this.setData({ usernamePassword: e.detail.value, usernameError: "" })
  },

  async saveUsername() {
    const username = this.data.usernameInput.trim()
    if (!/^[a-zA-Z][a-zA-Z0-9_]{3,31}$/.test(username)) {
      this.setData({ usernameError: "账号需以字母开头，4-32 位字母、数字或下划线" })
      return
    }
    if (username === this.data.username) {
      wx.showToast({ title: "账号未修改", icon: "none" })
      return
    }
    if (this.data.hasPassword && !this.data.usernamePassword) {
      this.setData({ usernameError: "请输入当前登录密码" })
      return
    }
    this.setData({ usernameSaving: true, usernameError: "" })
    try {
      const me = await api.updateUsername(username, this.data.usernamePassword || undefined)
      this.setData({
        me,
        username: me.username || "",
        panel: "",
        usernameInput: "",
        usernamePassword: "",
      })
      wx.showToast({ title: "账号已修改", icon: "success" })
    } catch (error) {
      this.setData({ usernameError: (error as Error).message })
    } finally {
      this.setData({ usernameSaving: false })
    }
  },

  onPhoneInput(e: any) {
    this.setData({ phoneInput: e.detail.value, phoneError: "" })
  },

  onPhonePasswordInput(e: any) {
    this.setData({ phonePassword: e.detail.value, phoneError: "" })
  },

  async savePhone() {
    const phone = this.data.phoneInput.trim()
    if (!/^1[3-9]\d{9}$/.test(phone)) {
      this.setData({ phoneError: "请输入正确的 11 位手机号" })
      return
    }
    if (this.data.phone && !this.data.hasPassword) {
      this.setData({ phoneError: "请先设置登录密码，再更换手机号" })
      return
    }
    if (this.data.phone && !this.data.phonePassword) {
      this.setData({ phoneError: "请输入当前登录密码" })
      return
    }
    this.setData({ phoneSaving: true, phoneError: "" })
    try {
      const me = await api.updatePhone(phone, this.data.phonePassword || undefined)
      this.setData({
        me,
        phone: me.phone || "",
        hasPassword: me.hasPassword === true,
        panel: "",
        phoneInput: "",
        phonePassword: "",
      })
      wx.showToast({ title: "手机号已保存", icon: "success" })
    } catch (error) {
      this.setData({ phoneError: (error as Error).message })
    } finally {
      this.setData({ phoneSaving: false })
    }
  },

  onEmailInput(e: any) {
    this.setData({ emailInput: e.detail.value, emailError: "" })
  },

  onEmailCodeInput(e: any) {
    this.setData({ emailCode: e.detail.value, emailError: "" })
  },

  onEmailPasswordInput(e: any) {
    this.setData({ emailPassword: e.detail.value, emailError: "" })
  },

  async sendEmailCode() {
    const email = this.data.emailInput.trim()
    if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) {
      this.setData({ emailError: "请输入正确的邮箱地址" })
      return
    }
    this.setData({ emailCodeSending: true, emailError: "" })
    try {
      await api.sendEmailCode(email)
      wx.showToast({ title: "验证码已发送", icon: "success" })
      this.startEmailCountdown()
    } catch (error) {
      this.setData({ emailError: (error as Error).message })
    } finally {
      this.setData({ emailCodeSending: false })
    }
  },

  startEmailCountdown() {
    this.stopEmailCountdown()
    this.setData({ emailCountdown: 60 })
    this.emailTimer = setInterval(() => {
      const next = this.data.emailCountdown - 1
      if (next <= 0) {
        this.stopEmailCountdown()
        this.setData({ emailCountdown: 0 })
        return
      }
      this.setData({ emailCountdown: next })
    }, 1000)
  },

  stopEmailCountdown() {
    if (this.emailTimer) {
      clearInterval(this.emailTimer)
      this.emailTimer = 0
    }
  },

  async saveEmail() {
    const email = this.data.emailInput.trim()
    const code = this.data.emailCode.trim()
    if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) {
      this.setData({ emailError: "请输入正确的邮箱地址" })
      return
    }
    if (!/^\d{6}$/.test(code)) {
      this.setData({ emailError: "请输入 6 位数字验证码" })
      return
    }
    if (this.data.email && !this.data.hasPassword) {
      this.setData({ emailError: "请先设置登录密码，再更换邮箱" })
      return
    }
    if (this.data.email && !this.data.emailPassword) {
      this.setData({ emailError: "请输入当前登录密码" })
      return
    }
    this.setData({ emailSaving: true, emailError: "" })
    try {
      const me = await api.bindEmail(email, code, this.data.emailPassword || undefined)
      this.setData({
        me,
        email: me.email || "",
        emailVerified: me.emailVerified === true,
        hasPassword: me.hasPassword === true,
        panel: "",
        emailInput: "",
        emailCode: "",
        emailPassword: "",
        emailCountdown: 0,
      })
      this.stopEmailCountdown()
      wx.showToast({ title: "邮箱已保存", icon: "success" })
    } catch (error) {
      this.setData({ emailError: (error as Error).message })
    } finally {
      this.setData({ emailSaving: false })
    }
  },

  unbindEmail() {
    wx.showModal({
      title: "解绑邮箱",
      content: "解绑后管理员将无法通过邮箱联系你，确定解绑吗？",
      success: async (res) => {
        if (!res.confirm) {
          return
        }
        this.setData({ emailSaving: true, emailError: "" })
        try {
          const me = await api.unbindEmail()
          this.setData({
            me,
            email: "",
            emailVerified: false,
            panel: "",
            emailInput: "",
            emailCode: "",
            emailPassword: "",
          })
          wx.showToast({ title: "邮箱已解绑", icon: "success" })
        } catch (error) {
          this.setData({ emailError: (error as Error).message })
        } finally {
          this.setData({ emailSaving: false })
        }
      },
    })
  },

  onOldPasswordInput(e: any) {
    this.setData({ oldPassword: e.detail.value, passwordError: "" })
  },

  onNewPasswordInput(e: any) {
    this.setData({ newPassword: e.detail.value, passwordError: "" })
  },

  onConfirmPasswordInput(e: any) {
    this.setData({ confirmPassword: e.detail.value, passwordError: "" })
  },

  async savePassword() {
    const { hasPassword, oldPassword, newPassword, confirmPassword } = this.data
    if (hasPassword && !oldPassword) {
      this.setData({ passwordError: "请输入原密码" })
      return
    }
    if (newPassword.length < 8 || newPassword.length > 32) {
      this.setData({ passwordError: "密码长度需在 8-32 之间" })
      return
    }
    if (newPassword !== confirmPassword) {
      this.setData({ passwordError: "两次输入的新密码不一致" })
      return
    }
    this.setData({ passwordSaving: true, passwordError: "" })
    try {
      const me = await api.changePassword(oldPassword, newPassword)
      this.setData({
        me,
        hasPassword: me.hasPassword === true,
        panel: "",
        oldPassword: "",
        newPassword: "",
        confirmPassword: "",
      })
      wx.showToast({ title: "密码已保存", icon: "success" })
    } catch (error) {
      this.setData({ passwordError: (error as Error).message })
    } finally {
      this.setData({ passwordSaving: false })
    }
  },

  async bindWechat() {
    this.setData({ wxLoading: true, wxError: "" })
    try {
      const code = await getWxCode()
      const result = await api.bindWechat(code)
      this.setData({ wxBound: result.wxBound })
      wx.showToast({ title: result.message || "绑定成功", icon: "success" })
    } catch (error) {
      this.setData({ wxError: (error as Error).message })
    } finally {
      this.setData({ wxLoading: false })
    }
  },

  unbindWechat() {
    wx.showModal({
      title: "解绑微信",
      content: "解绑后该微信将不能一键登录本账号，仍可使用账号密码登录。确定解绑吗？",
      success: async (res) => {
        if (!res.confirm) {
          return
        }
        this.setData({ wxLoading: true, wxError: "" })
        try {
          const result = await api.unbindWechat()
          this.setData({ wxBound: result.wxBound })
          wx.showToast({ title: result.message || "已解绑", icon: "success" })
        } catch (error) {
          this.setData({ wxError: (error as Error).message })
        } finally {
          this.setData({ wxLoading: false })
        }
      },
    })
  },
})
