export const formatTime = (date: Date) => {
  const year = date.getFullYear()
  const month = date.getMonth() + 1
  const day = date.getDate()
  const hour = date.getHours()
  const minute = date.getMinutes()
  const second = date.getSeconds()

  return (
    [year, month, day].map(formatNumber).join('/') +
    ' ' +
    [hour, minute, second].map(formatNumber).join(':')
  )
}

const formatNumber = (n: number) => {
  const s = n.toString()
  return s[1] ? s : '0' + s
}

// 拆分"我发布的注意事项"回执：正文与末尾的已发送给信息
export const splitNotice = (content: string, bizType: string) => {
  const text = content || ''
  if (bizType !== 'TEAM_NOTICE_SENT') {
    return { body: text, receipt: '' }
  }
  const idx = text.lastIndexOf('\n\n已发送给：')
  if (idx >= 0) {
    return { body: text.slice(0, idx), receipt: text.slice(idx + 2) }
  }
  // 兼容旧格式：已发送给：xxx。内容：yyy
  if (text.indexOf('已发送给：') === 0) {
    const split = text.indexOf('。内容：')
    if (split >= 0) {
      return { body: text.slice(split + 4), receipt: text.slice(0, split) }
    }
  }
  return { body: text, receipt: '' }
}
