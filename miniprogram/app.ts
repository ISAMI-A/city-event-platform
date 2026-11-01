// app.ts
App<IAppOption>({
  globalData: {
    // 手机真机调试需使用电脑局域网 IP，不能使用 127.0.0.1
    baseUrl: "http://10.123.159.42:8080",
    token: "",
    role: "",
    nickname: "",
  },
  onLaunch() {
    const token = wx.getStorageSync("token") as string
    const role = wx.getStorageSync("role") as string
    const nickname = wx.getStorageSync("nickname") as string
    if (token) {
      this.globalData.token = token
      this.globalData.role = role
      this.globalData.nickname = nickname
    }
  },
})
