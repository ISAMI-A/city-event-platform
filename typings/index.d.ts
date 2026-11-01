/// <reference path="./types/index.d.ts" />

interface IAppOption {
  globalData: {
    baseUrl: string
    token: string
    role: string
    nickname: string
    userInfo?: WechatMiniprogram.UserInfo
  }
  userInfoReadyCallback?: WechatMiniprogram.GetUserInfoSuccessCallback
}
