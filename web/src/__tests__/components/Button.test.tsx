import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { Button } from '../../components/ui/Button'

describe('Button', () => {
  it('renders children text', () => {
    render(<Button>登录</Button>)
    expect(screen.getByText('登录')).toBeInTheDocument()
  })

  it('calls onClick when clicked', () => {
    const onClick = vi.fn()
    render(<Button onClick={onClick}>点击</Button>)
    fireEvent.click(screen.getByText('点击'))
    expect(onClick).toHaveBeenCalledTimes(1)
  })

  it('does not call onClick when disabled', () => {
    const onClick = vi.fn()
    render(<Button onClick={onClick} disabled>点击</Button>)
    fireEvent.click(screen.getByText('点击'))
    expect(onClick).not.toHaveBeenCalled()
  })

  it('shows loading spinner when loading', () => {
    const { container } = render(<Button loading>加载中</Button>)
    const spinner = container.querySelector('.animate-spin')
    expect(spinner).toBeInTheDocument()
  })

  it('applies variant classes', () => {
    const { container } = render(<Button variant="primary">主要</Button>)
    const btn = container.querySelector('button')
    expect(btn?.className).toContain('bg-brand')
  })

  it('is disabled when loading', () => {
    render(<Button loading>加载中</Button>)
    expect(screen.getByText('加载中').closest('button')).toBeDisabled()
  })
})
