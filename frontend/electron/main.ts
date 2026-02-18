import { app, BrowserWindow, ipcMain, shell } from 'electron'
import path from 'path'

// 处理 Electron 的安全警告
process.env['ELECTRON_DISABLE_SECURITY_WARNINGS'] = 'true'

let mainWindow: BrowserWindow | null = null

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1400,
    height: 900,
    minWidth: 1200,
    minHeight: 700,
    title: 'AI员工协作系统',
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      nodeIntegration: false,
      contextIsolation: true,
      sandbox: false
    },
    // Notion 风格的窗口样式
    titleBarStyle: 'hiddenInset',
    trafficLightPosition: { x: 16, y: 16 },
    backgroundColor: '#ffffff',
    show: false
  })

  // 等待页面加载完成后再显示窗口，避免白屏闪烁
  mainWindow.once('ready-to-show', () => {
    mainWindow?.show()
  })

  // 开发环境加载 Vite 开发服务器
  if (process.env.NODE_ENV === 'development' || process.env.VITE_DEV_SERVER_URL) {
    const devServerUrl = process.env.VITE_DEV_SERVER_URL || 'http://localhost:3000'
    mainWindow.loadURL(devServerUrl)
    mainWindow.webContents.openDevTools()
  } else {
    // 生产环境加载打包后的文件
    mainWindow.loadFile(path.join(__dirname, '../dist/index.html'))
  }

  // 在外部浏览器中打开链接
  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    shell.openExternal(url)
    return { action: 'deny' }
  })

  mainWindow.on('closed', () => {
    mainWindow = null
  })
}

// 应用准备就绪时创建窗口
app.whenReady().then(() => {
  createWindow()

  app.on('activate', () => {
    // macOS 点击 dock 图标时重新创建窗口
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow()
    }
  })
})

// 所有窗口关闭时退出应用（Windows/Linux）
app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit()
  }
})

// IPC 通信处理器

// 获取应用版本
ipcMain.handle('get-app-version', () => {
  return app.getVersion()
})

// 获取系统信息
ipcMain.handle('get-system-info', () => {
  return {
    platform: process.platform,
    arch: process.arch,
    version: process.version,
    userDataPath: app.getPath('userData'),
    documentsPath: app.getPath('documents')
  }
})

// 打开文件对话框
ipcMain.handle('show-open-dialog', async (_event, options) => {
  const { dialog } = await import('electron')
  return dialog.showOpenDialog(mainWindow!, options)
})

// 保存文件对话框
ipcMain.handle('show-save-dialog', async (_event, options) => {
  const { dialog } = await import('electron')
  return dialog.showSaveDialog(mainWindow!, options)
})

// 显示系统通知
ipcMain.handle('show-notification', async (_event, { title, body }) => {
  const { Notification } = await import('electron')
  new Notification({ title, body }).show()
})
