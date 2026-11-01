interface TabItem {
  pagePath: string
  text: string
}

const RESIDENT_TABS: TabItem[] = [
  { pagePath: "/pages/events/events", text: "我的事件" },
  { pagePath: "/pages/profile/profile", text: "我的" },
]

const WORKER_TABS: TabItem[] = [
  { pagePath: "/pages/workorders/workorders", text: "我的工单" },
  { pagePath: "/pages/profile/profile", text: "我的" },
]

Component({
  data: {
    role: "",
    selected: 0,
    tabs: [] as TabItem[],
  },

  lifetimes: {
    attached() {
      ;(this as any).sync()
    },
  },

  methods: {
    sync(role?: string, selected?: number) {
      const currentRole = role || (wx.getStorageSync("role") as string) || ""
      this.setData({
        role: currentRole,
        tabs: currentRole === "WORKER" ? WORKER_TABS : RESIDENT_TABS,
        selected: typeof selected === "number" ? selected : this.data.selected,
      })
    },

    onTap(e: any) {
      const index = Number(e.currentTarget.dataset.index)
      const tab = this.data.tabs[index]
      if (!tab) {
        return
      }
      this.setData({ selected: index })
      wx.switchTab({ url: tab.pagePath })
    },
  },
})
