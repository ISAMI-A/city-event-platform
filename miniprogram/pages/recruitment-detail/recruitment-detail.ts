import { api, RecruitmentItem } from "../../utils/api"
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

Page({
  data: {
    loading: true,
    error: "",
    item: null as null | (RecruitmentItem & { deadlineText: string }),
    cover: "",
    note: "",
    applying: false,
  },

  recruitmentId: 0,

  onLoad(options: Record<string, string>) {
    this.recruitmentId = Number(options.id)
    this.load()
  },

  async load() {
    this.setData({ loading: true, error: "" })
    try {
      const item = await api.recruitmentDetail(this.recruitmentId)
      const cover = item.coverUrl ? await localImage(absoluteUrl(item.coverUrl)) : ""
      this.setData({
        item: { ...item, deadlineText: item.deadline || "长期有效" },
        cover,
      })
    } catch (error) {
      this.setData({ error: (error as Error).message })
    } finally {
      this.setData({ loading: false })
    }
  },

  onNoteInput(e: any) {
    this.setData({ note: e.detail.value })
  },

  apply() {
    const item = this.data.item
    if (!item || item.appliedByMe) {
      return
    }
    wx.showModal({
      title: "报名确认",
      content: `确认报名「${item.title}」吗？报名后可在个人资料页查看报名状态。`,
      success: async (res) => {
        if (!res.confirm) {
          return
        }
        this.setData({ applying: true, error: "" })
        try {
          await api.applyRecruitment(this.recruitmentId, this.data.note.trim())
          wx.showToast({ title: "报名成功", icon: "success" })
          this.setData({ note: "" })
          await this.load()
        } catch (error) {
          this.setData({ error: (error as Error).message })
        } finally {
          this.setData({ applying: false })
        }
      },
    })
  },
})
