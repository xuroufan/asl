/** Simple loading skeletons using Tailwind animate-pulse */

export function CardSkeleton({ rows = 4 }: { rows?: number }) {
  return (
    <div className="card p-4 animate-pulse space-y-3">
      <div className="h-5 bg-gray-800/60 rounded w-1/3" />
      {Array.from({ length: rows }).map((_, i) => (
        <div key={i} className="h-10 bg-gray-800/40 rounded" />
      ))}
    </div>
  )
}

export function ListSkeleton({ count = 5 }: { count?: number }) {
  return (
    <div className="space-y-3 animate-pulse">
      {Array.from({ length: count }).map((_, i) => (
        <div key={i} className="flex items-center gap-3 p-3">
          <div className="w-10 h-10 bg-gray-800/60 rounded-full" />
          <div className="flex-1 space-y-2">
            <div className="h-3 bg-gray-800/60 rounded w-1/3" />
            <div className="h-3 bg-gray-800/40 rounded w-1/2" />
          </div>
          <div className="h-8 w-20 bg-gray-800/40 rounded" />
        </div>
      ))}
    </div>
  )
}

export function MarketChartSkeleton() {
  return (
    <div className="card p-4 animate-pulse space-y-3">
      <div className="flex justify-between">
        <div className="h-6 bg-gray-800/60 rounded w-24" />
        <div className="h-6 bg-gray-800/40 rounded w-32" />
      </div>
      <div className="h-[320px] bg-gray-800/20 rounded-lg" />
    </div>
  )
}
