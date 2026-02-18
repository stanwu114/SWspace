import { create } from 'zustand'
import { devtools, persist } from 'zustand/middleware'
import type { Project, ProjectStatus } from '@/types'

interface ProjectState {
  // 状态
  projects: Project[]
  currentProject: Project | null
  loading: boolean
  error: string | null

  // 操作
  setProjects: (projects: Project[]) => void
  setCurrentProject: (project: Project | null) => void
  setLoading: (loading: boolean) => void
  setError: (error: string | null) => void
  
  // 筛选
  filterByStatus: (status: ProjectStatus | null) => Project[]
  filterByCustomer: (customerId: string) => Project[]
}

export const useProjectStore = create<ProjectState>()(
  devtools(
    persist(
      (set, get) => ({
        // 初始状态
        projects: [],
        currentProject: null,
        loading: false,
        error: null,

        // 操作方法
        setProjects: (projects) => set({ projects }),
        setCurrentProject: (project) => set({ currentProject: project }),
        setLoading: (loading) => set({ loading }),
        setError: (error) => set({ error }),

        // 筛选方法
        filterByStatus: (status) => {
          const { projects } = get()
          if (!status) return projects
          return projects.filter((p) => p.status === status)
        },
        filterByCustomer: (customerId) => {
          const { projects } = get()
          return projects.filter((p) => p.customerId === customerId)
        },
      }),
      {
        name: 'project-storage',
        partialize: (state) => ({ 
          currentProject: state.currentProject 
        }),
      }
    ),
    { name: 'ProjectStore' }
  )
)
