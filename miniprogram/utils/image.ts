/**
 * 微信真机上 <image> 组件已不支持 http 图片，会直接报“图片链接不再支持 HTTP 协议”。
 * 开发/内网环境后端只有 http 时，先用 wx.downloadFile 下载到本地临时文件，再用本地路径渲染与预览。
 * 同一地址只下载一次；下载失败时回退原地址，交给调用方兜底。
 */
const cache: Record<string, string> = {}

function needLocal(url: string) {
  return url.indexOf("http://") === 0
}

export function localImage(url?: string): Promise<string> {
  if (!url) {
    return Promise.resolve("")
  }
  if (!needLocal(url)) {
    return Promise.resolve(url)
  }
  const cached = cache[url]
  if (cached) {
    return Promise.resolve(cached)
  }
  return new Promise<string>((resolve) => {
    wx.downloadFile({
      url,
      success: (res) => {
        if (res.statusCode === 200 && res.tempFilePath) {
          cache[url] = res.tempFilePath
          resolve(res.tempFilePath)
          return
        }
        resolve(url)
      },
      fail: () => resolve(url),
    })
  })
}

export function localImages(urls: string[]): Promise<string[]> {
  return Promise.all(urls.map((url) => localImage(url)))
}
