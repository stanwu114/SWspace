import { Key, Bell, Palette, Database, Info, AlertCircle, CheckCircle, Loader2, RefreshCw, Server, Wifi, WifiOff } from 'lucide-react'
import { useState, useEffect, useCallback } from 'react'
import api from '@/services/api'

interface ModelInfo {
  id: string
  name: string
  ownedBy: string
}

interface NotificationSettingsProps {
  isRestarting: boolean
  setIsRestarting: (value: boolean) => void
  setSaveMsg: (msg: { type: 'success' | 'error' | 'info'; text: string } | null) => void
  waitForRestart: () => Promise<boolean>
}

// 通知设置子组件
function NotificationSettings({ isRestarting, setIsRestarting, setSaveMsg, waitForRestart }: NotificationSettingsProps) {
  const [feishuWebhook, setFeishuWebhook] = useState('')
  const [telegramToken, setTelegramToken] = useState('')
  const [telegramChatId, setTelegramChatId] = useState('')

  const handleSave = async () => {
    try {
      setIsRestarting(true)
      setSaveMsg({ type: 'info', text: '正在保存配置并重启服务...' })
      
      const r = await api.system.saveNotificationConfig(
        feishuWebhook || undefined,
        telegramToken || undefined,
        telegramChatId || undefined
      ) as any
      
      if (r?.code === 200) {
        setSaveMsg({ type: 'info', text: '配置已保存，正在等待服务重启...' })
        await waitForRestart()
      } else {
        setIsRestarting(false)
        setSaveMsg({ type: 'error', text: r?.message || '保存配置失败' })
        setTimeout(() => setSaveMsg(null), 5000)
      }
    } catch (e: any) {
      setIsRestarting(false)
      setSaveMsg({ type: 'error', text: e?.message || '保存配置失败' })
      setTimeout(() => setSaveMsg(null), 5000)
    }
  }

  return (
    <div>
      <div className="px-5 py-4 border-b border-gray-100">
        <h3 className="text-sm font-semibold text-gray-900">通知设置</h3>
        <p className="text-xs text-gray-400 mt-0.5">配置飞书、Telegram 等消息通道</p>
      </div>
      <div className="p-5 space-y-4">
        <div>
          <label className="block text-sm text-gray-600 mb-1.5">飞书 Webhook</label>
          <input 
            type="text" 
            value={feishuWebhook}
            onChange={e => setFeishuWebhook(e.target.value)}
            placeholder="https://open.feishu.cn/..." 
            disabled={isRestarting}
            className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm disabled:bg-gray-50" />
        </div>
        <div>
          <label className="block text-sm text-gray-600 mb-1.5">Telegram Bot Token</label>
          <input 
            type="text" 
            value={telegramToken}
            onChange={e => setTelegramToken(e.target.value)}
            placeholder="输入 Bot Token" 
            disabled={isRestarting}
            className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm disabled:bg-gray-50" />
        </div>
        <div>
          <label className="block text-sm text-gray-600 mb-1.5">Telegram Chat ID</label>
          <input 
            type="text" 
            value={telegramChatId}
            onChange={e => setTelegramChatId(e.target.value)}
            placeholder="输入 Chat ID" 
            disabled={isRestarting}
            className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm disabled:bg-gray-50" />
        </div>
        <button 
          onClick={handleSave} 
          disabled={isRestarting}
          className="px-3 py-2 text-sm text-white bg-brand-500 hover:bg-brand-600 rounded-lg disabled:opacity-50 flex items-center gap-1.5">
          {isRestarting ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : null}
          {isRestarting ? '保存并重启中...' : '保存'}
        </button>
      </div>
    </div>
  )
}

const tabs = [
  { key: 'ai', label: 'AI 模型', icon: Key },
  { key: 'notification', label: '通知', icon: Bell },
  { key: 'appearance', label: '外观', icon: Palette },
  { key: 'data', label: '数据', icon: Database },
  { key: 'about', label: '关于', icon: Info },
]

export default function Settings() {
  const [activeTab, setActiveTab] = useState('ai')
  const [healthStatus, setHealthStatus] = useState<any>(null)
  const [systemInfo, setSystemInfo] = useState<any>(null)
  const [backups, setBackups] = useState<any[]>([])
  const [backupLoading, setBackupLoading] = useState(false)
  const [backupMsg, setBackupMsg] = useState<{ type: 'success' | 'error'; text: string } | null>(null)
  const [healthLoading, setHealthLoading] = useState(false)
  
  // AI 配置状态
  const [aiConfig, setAiConfig] = useState({ endpoint: 'https://api.moonshot.cn/v1', apiKey: '', model: '' })
  const [models, setModels] = useState<ModelInfo[]>([])
  const [modelsLoading, setModelsLoading] = useState(false)
  const [testResult, setTestResult] = useState<{ success: boolean; message: string; latencyMs?: number } | null>(null)
  const [testLoading, setTestLoading] = useState(false)
  const [saveMsg, setSaveMsg] = useState<{ type: 'success' | 'error' | 'info'; text: string } | null>(null)
  const [isRestarting, setIsRestarting] = useState(false)
  const [restartProgress, setRestartProgress] = useState(0)

  // 检测服务是否恢复
  const checkServiceRecovery = useCallback(async () => {
    try {
      const r = await api.system.health() as any
      if (r?.code === 200) {
        return true
      }
    } catch {
      // 服务还未恢复
    }
    return false
  }, [])

  // 等待服务重启完成
  const waitForRestart = useCallback(async () => {
    setRestartProgress(0)
    const maxAttempts = 30 // 最多等待30秒
    const interval = 1000 // 每秒检查一次
    
    for (let i = 0; i < maxAttempts; i++) {
      setRestartProgress(Math.round((i / maxAttempts) * 100))
      
      // 等待一段时间再检查
      await new Promise(resolve => setTimeout(resolve, interval))
      
      if (await checkServiceRecovery()) {
        setRestartProgress(100)
        setIsRestarting(false)
        setSaveMsg({ type: 'success', text: '系统重启成功，配置已生效' })
        setTimeout(() => setSaveMsg(null), 5000)
        return true
      }
    }
    
    setIsRestarting(false)
    setSaveMsg({ type: 'error', text: '等待服务重启超时，请手动刷新页面检查' })
    return false
  }, [checkServiceRecovery])

  useEffect(() => {
    if (activeTab === 'about') loadSystemInfo()
    if (activeTab === 'data') loadBackups()
  }, [activeTab])

  const loadSystemInfo = async () => {
    try {
      setHealthLoading(true)
      const [healthRes, infoRes] = await Promise.all([
        api.system.health() as any,
        api.system.info() as any,
      ])
      if (healthRes?.code === 200) setHealthStatus(healthRes.data)
      if (infoRes?.code === 200) setSystemInfo(infoRes.data)
    } catch {} finally {
      setHealthLoading(false)
    }
  }

  const loadBackups = async () => {
    try {
      const r = await api.system.listBackups() as any
      if (r?.code === 200) setBackups(r.data || [])
    } catch {}
  }

  const handleBackup = async () => {
    try {
      setBackupLoading(true)
      setBackupMsg(null)
      const r = await api.system.backup() as any
      if (r?.code === 200) {
        setBackupMsg({ type: 'success', text: '备份成功' })
        loadBackups()
      } else {
        setBackupMsg({ type: 'error', text: r?.message || '备份失败' })
      }
    } catch (e: any) {
      setBackupMsg({ type: 'error', text: e?.message || '备份失败，请检查后端服务' })
    } finally {
      setBackupLoading(false)
    }
  }

  // 获取模型列表
  const fetchModels = async () => {
    if (!aiConfig.endpoint || !aiConfig.apiKey) {
      setSaveMsg({ type: 'error', text: '请先输入 API 端点和 API Key' })
      setTimeout(() => setSaveMsg(null), 3000)
      return
    }
    
    try {
      setModelsLoading(true)
      setSaveMsg(null)
      const r = await api.system.listModels(aiConfig.endpoint, aiConfig.apiKey) as any
      if (r?.code === 200 && r.data) {
        setModels(r.data)
        if (r.data.length > 0 && !aiConfig.model) {
          setAiConfig(prev => ({ ...prev, model: r.data[0].id }))
        }
        setSaveMsg({ type: 'success', text: `获取到 ${r.data.length} 个可用模型` })
      } else {
        setSaveMsg({ type: 'error', text: r?.message || '获取模型列表失败' })
      }
    } catch (e: any) {
      setSaveMsg({ type: 'error', text: e?.message || '获取模型列表失败' })
    } finally {
      setModelsLoading(false)
      setTimeout(() => setSaveMsg(null), 3000)
    }
  }

  // 测试连接
  const testConnection = async () => {
    if (!aiConfig.endpoint || !aiConfig.apiKey) {
      setTestResult({ success: false, message: '请先输入 API 端点和 API Key' })
      return
    }
    
    try {
      setTestLoading(true)
      setTestResult(null)
      const r = await api.system.testLLM(aiConfig.endpoint, aiConfig.apiKey) as any
      if (r?.code === 200 && r.data) {
        setTestResult(r.data)
      } else {
        setTestResult({ success: false, message: r?.message || '测试失败' })
      }
    } catch (e: any) {
      setTestResult({ success: false, message: e?.message || '连接失败' })
    } finally {
      setTestLoading(false)
    }
  }

  const handleSaveAI = async () => {
    if (!aiConfig.model) {
      setSaveMsg({ type: 'error', text: '请选择模型' })
      setTimeout(() => setSaveMsg(null), 3000)
      return
    }
    
    try {
      setIsRestarting(true)
      setSaveMsg({ type: 'info', text: '正在保存配置并重启服务...' })
      
      // 调用后端保存配置接口（保存后会自动重启）
      const r = await api.system.saveAIConfig(aiConfig.endpoint, aiConfig.apiKey, aiConfig.model) as any
      
      if (r?.code === 200) {
        setSaveMsg({ type: 'info', text: '配置已保存，正在等待服务重启...' })
        // 等待服务重启完成
        await waitForRestart()
      } else {
        setIsRestarting(false)
        setSaveMsg({ type: 'error', text: r?.message || '保存配置失败' })
        setTimeout(() => setSaveMsg(null), 5000)
      }
    } catch (e: any) {
      setIsRestarting(false)
      setSaveMsg({ type: 'error', text: e?.message || '保存配置失败' })
      setTimeout(() => setSaveMsg(null), 5000)
    }
  }

  return (
    <div className="p-6">
      <h1 className="text-lg font-semibold text-gray-900 mb-4">设置</h1>

      <div className="flex gap-6">
        {/* Tabs */}
        <div className="w-40 shrink-0 space-y-0.5">
          {tabs.map(tab => {
            const Icon = tab.icon
            return (
              <button key={tab.key} onClick={() => setActiveTab(tab.key)}
                className={`flex items-center gap-2.5 w-full px-3 py-2 rounded-lg text-sm transition-colors ${
                  activeTab === tab.key ? 'bg-brand-50 text-brand-600 font-medium' : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                }`}>
                <Icon className={`w-4 h-4 ${activeTab === tab.key ? 'text-brand-500' : ''}`} />
                {tab.label}
              </button>
            )
          })}
        </div>

        {/* Content */}
        <div className="flex-1 bg-white border border-gray-200 rounded-lg">
          {activeTab === 'ai' && (
            <div>
              <div className="px-5 py-4 border-b border-gray-100">
                <h3 className="text-sm font-semibold text-gray-900">AI 模型配置</h3>
                <p className="text-xs text-gray-400 mt-0.5">配置 AI 员工使用的语言模型</p>
              </div>
              <div className="p-5 space-y-4">
                {/* 消息提示 */}
                {saveMsg && (
                  <div className={`px-3 py-2 rounded-lg flex items-center gap-2 text-sm ${
                    saveMsg.type === 'success' ? 'bg-emerald-50 text-emerald-600 border border-emerald-200' : 
                    saveMsg.type === 'info' ? 'bg-blue-50 text-blue-600 border border-blue-200' :
                    'bg-red-50 text-red-600 border border-red-200'
                  }`}>
                    {saveMsg.type === 'success' ? <CheckCircle className="w-4 h-4" /> : 
                     saveMsg.type === 'info' ? <Loader2 className="w-4 h-4 animate-spin" /> :
                     <AlertCircle className="w-4 h-4" />}
                    {saveMsg.text}
                  </div>
                )}

                {/* 重启进度条 */}
                {isRestarting && (
                  <div className="space-y-2">
                    <div className="flex justify-between text-xs text-gray-500">
                      <span>服务重启中...</span>
                      <span>{restartProgress}%</span>
                    </div>
                    <div className="h-2 bg-gray-100 rounded-full overflow-hidden">
                      <div 
                        className="h-full bg-brand-500 transition-all duration-1000 ease-linear"
                        style={{ width: `${restartProgress}%` }}
                      />
                    </div>
                  </div>
                )}

                {/* 连接测试结果 */}
                {testResult && (
                  <div className={`px-3 py-2 rounded-lg flex items-center gap-2 text-sm ${
                    testResult.success ? 'bg-emerald-50 text-emerald-600 border border-emerald-200' : 'bg-red-50 text-red-600 border border-red-200'
                  }`}>
                    {testResult.success ? <Wifi className="w-4 h-4" /> : <WifiOff className="w-4 h-4" />}
                    <span className="flex-1">{testResult.message}</span>
                    {testResult.latencyMs !== undefined && testResult.latencyMs > 0 && (
                      <span className="text-xs opacity-70">{testResult.latencyMs}ms</span>
                    )}
                  </div>
                )}

                {/* API 端点 */}
                <div>
                  <label className="block text-sm text-gray-600 mb-1.5">API 端点</label>
                  <input type="text" value={aiConfig.endpoint} onChange={e => setAiConfig({ ...aiConfig, endpoint: e.target.value })}
                    placeholder="https://api.openai.com/v1"
                    className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm" />
                  <p className="text-xs text-gray-400 mt-1">支持 OpenAI、Moonshot、DeepSeek 等兼容接口</p>
                </div>

                {/* API Key */}
                <div>
                  <label className="block text-sm text-gray-600 mb-1.5">API Key</label>
                  <input type="password" value={aiConfig.apiKey} onChange={e => setAiConfig({ ...aiConfig, apiKey: e.target.value })}
                    placeholder="sk-****" className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm" />
                </div>

                {/* 测试和获取模型按钮 */}
                <div className="flex items-center gap-2">
                  <button onClick={testConnection} disabled={testLoading || !aiConfig.endpoint || !aiConfig.apiKey}
                    className="px-3 py-2 text-sm text-gray-600 border border-gray-200 hover:bg-gray-50 rounded-lg disabled:opacity-50 flex items-center gap-1.5">
                    {testLoading ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Wifi className="w-3.5 h-3.5" />}
                    测试连接
                  </button>
                  <button onClick={fetchModels} disabled={modelsLoading || !aiConfig.endpoint || !aiConfig.apiKey}
                    className="px-3 py-2 text-sm text-white bg-brand-500 hover:bg-brand-600 rounded-lg disabled:opacity-50 flex items-center gap-1.5">
                    {modelsLoading ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <RefreshCw className="w-3.5 h-3.5" />}
                    获取模型列表
                  </button>
                </div>

                {/* 模型选择 */}
                <div>
                  <label className="block text-sm text-gray-600 mb-1.5">模型</label>
                  {models.length > 0 ? (
                    <select value={aiConfig.model} onChange={e => setAiConfig({ ...aiConfig, model: e.target.value })}
                      className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm">
                      <option value="">请选择模型</option>
                      {models.map(m => (
                        <option key={m.id} value={m.id}>{m.name || m.id} {m.ownedBy && `(${m.ownedBy})`}</option>
                      ))}
                    </select>
                  ) : (
                    <div className="w-full px-3 py-2 border border-gray-200 rounded-lg text-sm text-gray-400">
                      请先输入端点和 API Key，然后点击"获取模型列表"
                    </div>
                  )}
                </div>

                {/* 保存按钮 */}
                <button onClick={handleSaveAI} disabled={!aiConfig.model || isRestarting}
                  className="px-3 py-2 text-sm text-white bg-brand-500 hover:bg-brand-600 rounded-lg disabled:opacity-50 flex items-center gap-1.5">
                  {isRestarting ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : null}
                  {isRestarting ? '保存并重启中...' : '保存配置'}
                </button>
              </div>
            </div>
          )}

          {activeTab === 'notification' && (
            <NotificationSettings isRestarting={isRestarting} setIsRestarting={setIsRestarting} setSaveMsg={setSaveMsg} waitForRestart={waitForRestart} />
          )}

          {activeTab === 'appearance' && (
            <div className="p-5">
              <h3 className="text-sm font-semibold text-gray-900 mb-1">外观设置</h3>
              <p className="text-xs text-gray-400">当前为默认主题</p>
            </div>
          )}

          {activeTab === 'data' && (
            <div>
              <div className="px-5 py-4 border-b border-gray-100">
                <h3 className="text-sm font-semibold text-gray-900">数据管理</h3>
                <p className="text-xs text-gray-400 mt-0.5">数据库备份与恢复</p>
              </div>
              <div className="p-5 space-y-4">
                {backupMsg && (
                  <div className={`px-3 py-2 rounded-lg flex items-center gap-2 text-sm ${
                    backupMsg.type === 'success' ? 'bg-emerald-50 text-emerald-600 border border-emerald-200' : 'bg-red-50 text-red-600 border border-red-200'
                  }`}>
                    {backupMsg.type === 'success' ? <CheckCircle className="w-4 h-4" /> : <AlertCircle className="w-4 h-4" />}
                    {backupMsg.text}
                  </div>
                )}
                <div className="flex items-center gap-2">
                  <button onClick={handleBackup} disabled={backupLoading}
                    className="px-3 py-2 text-sm text-white bg-brand-500 hover:bg-brand-600 rounded-lg disabled:opacity-50 flex items-center gap-1.5">
                    {backupLoading ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Database className="w-3.5 h-3.5" />}
                    {backupLoading ? '备份中...' : '手动备份'}
                  </button>
                  <button onClick={loadBackups} className="px-3 py-2 text-sm text-gray-600 border border-gray-200 hover:bg-gray-50 rounded-lg flex items-center gap-1.5">
                    <RefreshCw className="w-3.5 h-3.5" /> 刷新列表
                  </button>
                </div>
                {backups.length > 0 && (
                  <div>
                    <h4 className="text-xs font-medium text-gray-500 mb-2">备份记录</h4>
                    <div className="space-y-1">
                      {backups.map((b: any, i: number) => (
                        <div key={i} className="flex items-center justify-between px-3 py-2 rounded-lg bg-gray-50 text-sm">
                          <span className="text-gray-700">{b.fileName || b.name || `备份 ${i + 1}`}</span>
                          <span className="text-xs text-gray-400">{b.createdAt ? new Date(b.createdAt).toLocaleString('zh-CN') : ''}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
                {backups.length === 0 && !backupLoading && (
                  <p className="text-xs text-gray-400">暂无备份记录</p>
                )}
              </div>
            </div>
          )}

          {activeTab === 'about' && (
            <div className="p-5">
              <div className="text-center py-4">
                <div className="w-12 h-12 bg-brand-500 rounded-lg flex items-center justify-center mx-auto mb-3">
                  <span className="text-white text-base font-bold">AI</span>
                </div>
                <h3 className="text-sm font-bold text-gray-900 mb-0.5">AI 员工协作系统</h3>
                <p className="text-xs text-gray-400 mb-3">{systemInfo?.version ? `v${systemInfo.version}` : 'v1.0.0'}</p>
                <p className="text-sm text-gray-500 max-w-sm mx-auto">超级个人 + AI 员工团队协作系统，赋能政企智慧城市行业专家。</p>
              </div>

              {/* System health */}
              <div className="mt-6 border-t border-gray-100 pt-4">
                <div className="flex items-center justify-between mb-3">
                  <h4 className="text-xs font-medium text-gray-500 flex items-center gap-1.5">
                    <Server className="w-3.5 h-3.5" /> 系统状态
                  </h4>
                  <button onClick={loadSystemInfo} disabled={healthLoading}
                    className="text-xs text-brand-500 hover:text-brand-600 disabled:opacity-50">
                    {healthLoading ? '检查中...' : '刷新'}
                  </button>
                </div>

                {healthLoading && !healthStatus && (
                  <div className="flex justify-center py-4">
                    <div className="w-4 h-4 border-2 border-gray-200 border-t-brand-500 rounded-full animate-spin" />
                  </div>
                )}

                {healthStatus && (
                  <div className="space-y-2">
                    <div className="flex items-center justify-between px-3 py-2 rounded-lg bg-gray-50 text-sm">
                      <span className="text-gray-600">服务状态</span>
                      <span className={`text-xs font-medium ${healthStatus.status === 'UP' ? 'text-emerald-600' : 'text-red-500'}`}>
                        {healthStatus.status === 'UP' ? '运行中' : '异常'}
                      </span>
                    </div>
                    <div className="flex items-center justify-between px-3 py-2 rounded-lg bg-gray-50 text-sm">
                      <span className="text-gray-600">数据库</span>
                      <span className={`text-xs font-medium ${healthStatus.database === 'UP' ? 'text-emerald-600' : 'text-red-500'}`}>
                        {healthStatus.database === 'UP' ? '正常' : healthStatus.database || '未知'}
                      </span>
                    </div>
                    {healthStatus.uptime && (
                      <div className="flex items-center justify-between px-3 py-2 rounded-lg bg-gray-50 text-sm">
                        <span className="text-gray-600">运行时间</span>
                        <span className="text-xs text-gray-500">{healthStatus.uptime}</span>
                      </div>
                    )}
                    {healthStatus.memory && (
                      <div className="flex items-center justify-between px-3 py-2 rounded-lg bg-gray-50 text-sm">
                        <span className="text-gray-600">内存使用</span>
                        <span className="text-xs text-gray-500">{healthStatus.memory.used} / {healthStatus.memory.max}</span>
                      </div>
                    )}
                  </div>
                )}

                {!healthStatus && !healthLoading && (
                  <div className="text-center py-4">
                    <AlertCircle className="w-5 h-5 text-gray-300 mx-auto mb-1" />
                    <p className="text-xs text-gray-400">无法连接到后端服务</p>
                  </div>
                )}

                {systemInfo && (
                  <div className="mt-3 space-y-2">
                    {systemInfo.javaVersion && (
                      <div className="flex items-center justify-between px-3 py-2 rounded-lg bg-gray-50 text-sm">
                        <span className="text-gray-600">Java 版本</span>
                        <span className="text-xs text-gray-500">{systemInfo.javaVersion}</span>
                      </div>
                    )}
                    {systemInfo.osName && (
                      <div className="flex items-center justify-between px-3 py-2 rounded-lg bg-gray-50 text-sm">
                        <span className="text-gray-600">操作系统</span>
                        <span className="text-xs text-gray-500">{systemInfo.osName} ({systemInfo.osArch})</span>
                      </div>
                    )}
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
