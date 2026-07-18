import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { Badge } from '../../components/ui/Badge'

describe('Badge', () => {
  it('shows positive value with + sign', () => {
    render(<Badge value={5.25} suffix="%" />)
    expect(screen.getByText('+5.25%')).toBeInTheDocument()
  })

  it('shows negative value without + sign', () => {
    render(<Badge value={-3.14} suffix="%" />)
    expect(screen.getByText('-3.14%')).toBeInTheDocument()
  })

  it('shows zero without sign', () => {
    render(<Badge value={0} />)
    expect(screen.getByText('0.00')).toBeInTheDocument()
  })

  it('renders with prefix', () => {
    const { container } = render(<Badge value={100} prefix="$" />)
    expect(container.querySelector('.badge-up')).toBeInTheDocument()
  })
})
