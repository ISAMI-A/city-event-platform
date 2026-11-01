import { api, EventDetail } from "../../utils/api"
import { localImage } from "../../utils/image"

const STATUS_TEXT: Record<string, string> = {
  PENDING_REVIEW: "待审核",
  REJECTED: "已驳回",
  CANCELLED: "已撤销",
  DISPATCHED: "已派单",
  PROCESSING: "处理中",
  PENDING_ACCEPTANCE: "待验收",
  CLOSED: "已关闭",
}

function statusText(status?: string) {
  if (!status) {
    return "-"
  }
  return STATUS_TEXT[status] || status
}

type TimelineRow = EventDetail["timeline"][number] & {
  fromStatusText: string
  toStatusText: string
}

type WorkOrderBrief = NonNullable<EventDetail["workOrder"]> & { statusText: string }

type DetailView = EventDetail & {
  statusText: string
  timeline: TimelineRow[]
  workOrder?: WorkOrderBrief
}

const RATING_TAGS = ["响应快", "态度好", "处理彻底", "沟通顺畅"]

Page({
  data: {
    detail: null as null | DetailView,
    loading: true,
    cancelling: false,
    error: "",
    rating: null as null | { rating: number; tags: string; comment: string; createTime?: string },
    ratingValue: 5,
    tagOptions: RATING_TAGS.map((name) => ({ name, active: false })),
    ratingComment: "",
    ratingSubmitting: false,
    starList: [1, 2, 3, 4, 5],
  },

  eventId: 0,

  onLoad(options: Record<string, string>) {
    this.eventId = Number(options.id)
    this.load(this.eventId)
  },

  async load(id: number) {
    this.setData({ loading: true, error: "" })
    try {
      const detail = await api.eventDetail(id)
      const baseUrl = getApp<IAppOption>().globalData.baseUrl
      const attachments = await Promise.all(
        detail.attachments.map(async (item) => ({
          ...item,
          accessUrl: await localImage(item.accessUrl.startsWith("http") ? item.accessUrl : baseUrl + item.accessUrl),
        })),
      )
      const timeline = detail.timeline.map((item) => ({
        ...item,
        fromStatusText: statusText(item.fromStatus),
        toStatusText: statusText(item.toStatus),
      }))
      const workOrder = detail.workOrder
        ? { ...detail.workOrder, statusText: statusText(detail.workOrder.status) }
        : undefined
      let rating: { rating: number; tags: string; comment: string; createTime?: string } | null = null
      if (workOrder && workOrder.workOrderId && workOrder.status === "COMPLETED") {
        try {
          const orderDetail = await api.workOrderDetail(workOrder.workOrderId)
          if (orderDetail.rating) {
            rating = {
              rating: orderDetail.rating.rating,
              tags: orderDetail.rating.tags || "",
              comment: orderDetail.rating.comment || "",
              createTime: orderDetail.rating.createTime,
            }
          }
        } catch (e) {
          // 评价查询失败不阻断事件详情
        }
      }
      this.setData({
        detail: { ...detail, attachments, timeline, workOrder, statusText: statusText(detail.status) },
        rating,
      })
    } catch (error) {
      this.setData({ error: (error as Error).message })
    } finally {
      this.setData({ loading: false })
    }
  },

  cancelEvent() {
    wx.showModal({
      title: "撤销上报",
      content: "撤销后该事件将不再进入审核流程，确定撤销吗？",
      success: async (res) => {
        if (!res.confirm) {
          return
        }
        this.setData({ cancelling: true, error: "" })
        try {
          await api.cancelEvent(this.eventId)
          wx.showToast({ title: "已撤销", icon: "success" })
          setTimeout(() => wx.switchTab({ url: "/pages/events/events" }), 600)
        } catch (error) {
          this.setData({ error: (error as Error).message })
        } finally {
          this.setData({ cancelling: false })
        }
      },
    })
  },

  resubmit() {
    wx.navigateTo({ url: `/pages/report/report?mode=resubmit&id=${this.eventId}` })
  },

  selectStar(e: any) {
    this.setData({ ratingValue: Number(e.currentTarget.dataset.value) })
  },

  toggleTag(e: any) {
    const index = Number(e.currentTarget.dataset.index)
    const tagOptions = this.data.tagOptions.map((item, itemIndex) => (
      itemIndex === index ? { ...item, active: !item.active } : item
    ))
    this.setData({ tagOptions })
  },

  onRatingCommentInput(e: any) {
    this.setData({ ratingComment: e.detail.value })
  },

  async submitRating() {
    const detail = this.data.detail
    if (!detail || !detail.workOrder) {
      return
    }
    this.setData({ ratingSubmitting: true, error: "" })
    try {
      const tags = this.data.tagOptions.filter((item) => item.active).map((item) => item.name)
      await api.rateWorkOrder(detail.workOrder.workOrderId, this.data.ratingValue, tags, this.data.ratingComment.trim())
      wx.showToast({ title: "评价成功", icon: "success" })
      await this.load(this.eventId)
    } catch (error) {
      this.setData({ error: (error as Error).message })
    } finally {
      this.setData({ ratingSubmitting: false })
    }
  },
})
