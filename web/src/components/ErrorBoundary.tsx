import { Component, type ErrorInfo, type ReactNode } from 'react'
import { Button } from './ui/Button'
import { AlertTriangle } from 'lucide-react'

interface Props { children: ReactNode }
interface State { hasError: boolean; error: Error | null }

export class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false, error: null }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('ErrorBoundary:', error.message, info.componentStack)
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="flex flex-col items-center justify-center min-h-[300px] p-8 text-center">
          <AlertTriangle className="w-12 h-12 text-buy mb-4 opacity-70" />
          <h2 className="text-lg font-semibold text-gray-200 mb-2">页面出错了</h2>
          <p className="text-sm text-gray-500 mb-6 max-w-sm">
            {this.state.error?.message || '发生了未知错误'}
          </p>
          <Button onClick={() => this.setState({ hasError: false, error: null })}>
            重试
          </Button>
        </div>
      )
    }
    return this.props.children
  }
}
