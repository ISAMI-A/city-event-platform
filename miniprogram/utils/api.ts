interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

function request<T>(options: {
  url: string
  method?: "GET" | "POST" | "PUT" | "DELETE"
  data?: Record<string, unknown>
}): Promise<T> {
  const app = getApp<IAppOption>()
  return new Promise<T>((resolve, reject) => {
    wx.request({
      url: app.globalData.baseUrl + options.url,
      method: options.method || "GET",
      data: options.data,
      header: {
        Authorization: `Bearer ${app.globalData.token}`,
        "Content-Type": "application/json",
      },
      success: (res) => {
        if (res.statusCode === 401) {
          wx.removeStorageSync("token")
          wx.reLaunch({ url: "/pages/login/login" })
          reject(new Error("登录已失效，请重新登录"))
          return
        }
        const body = res.data as ApiResponse<T>
        if (body && body.code === 0) {
          resolve(body.data)
        } else {
          reject(new Error((body && body.message) || "请求失败"))
        }
      },
      fail: (error) => reject(new Error(error.errMsg || "网络异常，请检查后端服务")),
    })
  })
}

function upload(filePath: string, bizType: string): Promise<{ id: number; accessUrl: string }> {
  const app = getApp<IAppOption>()
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: `${app.globalData.baseUrl}/api/v1/attachments`,
      filePath,
      name: "file",
      formData: { bizType },
      header: { Authorization: `Bearer ${app.globalData.token}` },
      success: (res) => {
        try {
          const body = JSON.parse(res.data) as ApiResponse<{ id: number; accessUrl: string }>
          if (body.code === 0) {
            resolve(body.data)
          } else {
            reject(new Error(body.message))
          }
        } catch (e) {
          reject(new Error("上传响应解析失败"))
        }
      },
      fail: (error) => reject(new Error(error.errMsg || "上传失败")),
    })
  })
}

export const api = {
  login(username: string, password: string, client: string) {
    return request<LoginResult>({
      url: "/api/v1/auth/login",
      method: "POST",
      data: { username, password, client },
    })
  },
  wxLogin(code: string) {
    return request<LoginResult>({
      url: "/api/v1/auth/wx-login",
      method: "POST",
      data: { code },
    })
  },
  wxBind(bindTicket: string, username: string, password: string) {
    return request<LoginResult>({
      url: "/api/v1/auth/wx-bind",
      method: "POST",
      data: { bindTicket, username, password },
    })
  },
  wxRegister(bindTicket: string, nickname: string) {
    return request<LoginResult>({
      url: "/api/v1/auth/wx-register",
      method: "POST",
      data: { bindTicket, nickname },
    })
  },
  register(data: { username: string; password: string; nickname: string; phone: string }) {
    return request<LoginResult>({
      url: "/api/v1/auth/register",
      method: "POST",
      data,
    })
  },
  bindWechat(code: string) {
    return request<{ wxBound: boolean; message: string }>({
      url: "/api/v1/auth/wx/bind",
      method: "POST",
      data: { code },
    })
  },
  unbindWechat() {
    return request<{ wxBound: boolean; message: string }>({
      url: "/api/v1/auth/wx/unbind",
      method: "POST",
      data: {},
    })
  },
  updateProfile(nickname: string | undefined, avatarUrl: string | undefined) {
    return request<Record<string, unknown>>({
      url: "/api/v1/auth/me/profile",
      method: "PUT",
      data: { nickname, avatarUrl },
    })
  },
  updateUsername(username: string, password?: string) {
    return request<UserProfile>({
      url: "/api/v1/auth/me/username",
      method: "PUT",
      data: { username, password },
    })
  },
  updatePhone(phone: string, password?: string) {
    return request<UserProfile>({
      url: "/api/v1/auth/me/phone",
      method: "PUT",
      data: { phone, password },
    })
  },
  changePassword(oldPassword: string, newPassword: string) {
    return request<UserProfile>({
      url: "/api/v1/auth/me/password",
      method: "PUT",
      data: { oldPassword, newPassword },
    })
  },
  sendEmailCode(email: string) {
    return request<void>({
      url: "/api/v1/auth/email/code",
      method: "POST",
      data: { email },
    })
  },
  bindEmail(email: string, code: string, password?: string) {
    return request<UserProfile>({
      url: "/api/v1/auth/email/bind",
      method: "POST",
      data: { email, code, password },
    })
  },
  unbindEmail() {
    return request<UserProfile>({
      url: "/api/v1/auth/email/unbind",
      method: "POST",
      data: {},
    })
  },
  getMe() {
    return request<UserProfile>({ url: "/api/v1/auth/me" })
  },
  workerProfile() {
    return request<WorkerProfileInfo>({ url: "/api/v1/workers/me/profile" })
  },
  myTeam() {
    return request<WorkerItem[]>({ url: "/api/v1/workers/my-team" })
  },
  getNotifications() {
    return request<NotificationItem[]>({ url: "/api/v1/notifications/my" })
  },
  getNotification(id: number) {
    return request<NotificationItem>({ url: `/api/v1/notifications/${id}` })
  },
  unreadNotificationCount() {
    return request<number>({ url: "/api/v1/notifications/unread-count" })
  },
  readNotification(id: number) {
    return request<void>({ url: `/api/v1/notifications/${id}/read`, method: "POST", data: {} })
  },
  forwardNotice(workOrderId: number, memberId: number, note?: string) {
    return request<void>({
      url: "/api/v1/notifications/forward",
      method: "POST",
      data: { workOrderId, memberId, note },
    })
  },
  publishTeamNotice(content: string) {
    return request<number>({
      url: "/api/v1/notifications/team-notice",
      method: "POST",
      data: { content },
    })
  },
  teamWorkOrders(page: number, size: number, status?: string) {
    const suffix = status ? `&status=${status}` : ""
    return request<{ records: WorkOrderItem[]; total: number }>({
      url: `/api/v1/workorders/team?page=${page}&size=${size}${suffix}`,
    })
  },
  teamStats() {
    return request<TeamStat[]>({ url: "/api/v1/workorders/team/stats" })
  },
  workOrderLoads(ids: number[]) {
    return request<{ workerId: number; activeCount: number }[]>({
      url: `/api/v1/workorders/loads?ids=${ids.join(",")}`,
    })
  },
  logout() {
    return request<void>({ url: "/api/v1/auth/logout", method: "POST", data: {} })
  },
  analyze(description: string, address: string, imageUrls: string[]) {
    return request<{
      callId: number
      categoryCode: string
      description: string
      summary: string
      priority: string
      suggestion: string
      reasoning: string
      confidence: number
      degraded: boolean
      degradeReason: string
    }>({
      url: "/api/v1/ai/analyze",
      method: "POST",
      data: { description, address, imageUrls },
    })
  },
  createEvent(data: Record<string, unknown>) {
    return request<{ id: number; eventNo: string; status: string }>({
      url: "/api/v1/events",
      method: "POST",
      data,
    })
  },
  myEvents(page: number, size: number) {
    return request<{ records: EventItem[]; total: number }>({
      url: `/api/v1/events/my?page=${page}&size=${size}`,
    })
  },
  eventDetail(id: number) {
    return request<EventDetail>({ url: `/api/v1/events/${id}` })
  },
  cancelEvent(id: number) {
    return request<Record<string, unknown>>({
      url: `/api/v1/events/${id}/cancel`,
      method: "POST",
      data: {},
    })
  },
  resubmitEvent(id: number, data: Record<string, unknown>) {
    return request<EventDetail>({
      url: `/api/v1/events/${id}/resubmit`,
      method: "PUT",
      data,
    })
  },
  batchAttachments(ids: number[]) {
    return request<AttachmentItem[]>({ url: `/api/v1/attachments/batch?ids=${ids.join(",")}` })
  },
  myWorkOrders(page: number, size: number) {
    return request<{ records: WorkOrderItem[]; total: number }>({
      url: `/api/v1/workorders/my?page=${page}&size=${size}`,
    })
  },
  workOrderDetail(id: number) {
    return request<WorkOrderDetail>({ url: `/api/v1/workorders/${id}` })
  },
  acceptWorkOrder(id: number) {
    return request<WorkOrderDetail>({ url: `/api/v1/workorders/${id}/accept`, method: "POST", data: {} })
  },
  cancelAcceptWorkOrder(id: number) {
    return request<WorkOrderDetail>({ url: `/api/v1/workorders/${id}/cancel-accept`, method: "POST", data: {} })
  },
  withdrawWorkOrderResult(id: number) {
    return request<WorkOrderDetail>({ url: `/api/v1/workorders/${id}/withdraw`, method: "POST", data: {} })
  },
  deleteWorkOrderLog(id: number, logId: number) {
    return request<WorkOrderDetail>({ url: `/api/v1/workorders/${id}/logs/${logId}`, method: "DELETE" })
  },
  generateWorkOrderDraft(id: number, attachmentIds: number[]) {
    return request<DraftGenerateResult>({
      url: `/api/v1/workorders/${id}/ai-draft`,
      method: "POST",
      data: { attachmentIds },
    })
  },
  submitWorkOrder(id: number, content: string, attachmentIds: number[]) {
    return request<WorkOrderDetail>({
      url: `/api/v1/workorders/${id}/submit`,
      method: "POST",
      data: { content, attachmentIds },
    })
  },
  saveWorkOrderProcess(id: number, content: string, attachmentIds: number[]) {
    return request<WorkOrderDetail>({
      url: `/api/v1/workorders/${id}/process`,
      method: "PUT",
      data: { content, attachmentIds },
    })
  },
  assignWorkOrder(id: number, assigneeId: number) {
    return request<WorkOrderDetail>({
      url: `/api/v1/workorders/${id}/assign`,
      method: "POST",
      data: { assigneeId },
    })
  },
  leaderReviewWorkOrder(id: number, pass: boolean, remark: string) {
    return request<WorkOrderDetail>({
      url: `/api/v1/workorders/${id}/leader-review`,
      method: "POST",
      data: { result: pass ? "PASS" : "REJECT", remark },
    })
  },
  rateWorkOrder(id: number, rating: number, tags: string[], comment: string) {
    return request<WorkOrderDetail>({
      url: `/api/v1/workorders/${id}/rating`,
      method: "POST",
      data: { rating, tags, comment },
    })
  },
  openRecruitments() {
    return request<RecruitmentItem[]>({ url: "/api/v1/recruitments/open" })
  },
  recruitmentDetail(id: number) {
    return request<RecruitmentItem>({ url: `/api/v1/recruitments/${id}` })
  },
  applyRecruitment(id: number, note: string) {
    return request<{ applyId: number; status: string }>({
      url: `/api/v1/recruitments/${id}/apply`,
      method: "POST",
      data: { note },
    })
  },
  upload,
}

export interface LoginResult {
  token?: string
  userId?: number
  nickname?: string
  avatarUrl?: string
  activeRole?: string
  roles?: string[]
  expireSeconds?: number
  needBind?: boolean
  bindTicket?: string
}

export interface UserProfile {
  userId: number
  username: string
  nickname: string
  avatarUrl?: string
  activeRole?: string
  roles?: string[]
  phone?: string
  email?: string
  emailVerified?: boolean
  hasPassword?: boolean
  wxBound?: boolean
}

export interface WorkerProfileInfo {
  userId?: number
  nickname?: string
  deptCode?: string
  deptName?: string
  areaCode?: string
  areaName?: string
  status?: string
  onDuty?: boolean
  accountStatus?: string
  workerType?: string
  leaderId?: number
  leaderName?: string
  workZone?: string
  teamSize?: number
}

export interface WorkerItem {
  id: number
  nickname: string
  phone?: string
  deptCode?: string
  deptName?: string
  workZone?: string
  areaCode?: string
  status?: string
  workerType?: string
  leaderId?: number
}

export interface TeamStat {
  workerId: number
  completedCount: number
  avgRating: number
  avgHours: number
  returnCount: number
  score: number
}

export interface RecruitmentItem {
  id: number
  title: string
  deptCode?: string
  deptName?: string
  workZone?: string
  headcount?: number
  requirement?: string
  description?: string
  status?: string
  deadline?: string
  coverUrl?: string
  createTime?: string
  applyCount?: number
  appliedByMe?: boolean
}

export interface AttachmentItem {
  id: number
  accessUrl: string
  originalName?: string
  contentType?: string
}

export interface EventItem {
  id: number
  eventNo: string
  title: string
  categoryCode: string
  priority: string
  status: string
  address: string
  createTime: string
  rejectReason?: string
}

export interface EventDetail extends EventItem {
  description: string
  areaCode?: string
  longitude?: number
  latitude?: number
  aiCategoryCode?: string
  aiPriority?: string
  aiSuggestion?: string
  aiReviewPriority?: string
  aiReviewReason?: string
  aiReviewImpact?: string
  aiReviewResponseMinutes?: number
  aiReviewConfidence?: number
  aiSnapshot?: {
    categoryCode: string
    priority: string
    summary: string
    suggestion: string
    confidence: number
  }
  attachments: { id: number; accessUrl: string }[]
  timeline: { fromStatus?: string; toStatus: string; action: string; remark?: string; createTime: string }[]
  workOrder?: { workOrderId: number; workOrderNo: string; status: string }
}

export interface DraftGenerateResult {
  content?: string
  degraded: boolean
  degradeReason?: string
  model?: string
  generatedAt?: string
}

export interface WorkOrderItem {
  id: number
  workOrderNo: string
  eventNo: string
  categoryCode: string
  priority: string
  status: string
  assignTime: string
  assigneeId?: number
  assigneeName?: string
  leaderId?: number
  leaderName?: string
}

export interface NotificationItem {
  id: number
  senderId?: number
  title: string
  content: string
  bizType: string
  bizId: number
  isRead: number
  createTime: string
}

export interface WorkOrderDetail extends WorkOrderItem {
  assigneeName: string
  returnCount: number
  acceptanceRemark?: string
  leaderReviewRemark?: string
  leaderReviewTime?: string
  rating?: {
    rating: number
    tags?: string
    comment?: string
    createTime?: string
  }
  event?: {
    eventId: number
    eventNo: string
    title: string
    description: string
    address: string
    status: string
    longitude?: number
    latitude?: number
  }
  /** 居民上报时的原始照片 */
  eventPhotos?: { id: number; accessUrl: string; contentType?: string; fileSize?: number }[]
  logs: { id: number; action: string; operatorId?: number; content?: string; attachmentIds?: string; createTime: string }[]
  acceptance?: { result: string; remark?: string }
}
