export default function StatCard({ label, value, tone = 'blue', icon: IconComp, breakdown }) {
  return (
    <article className={`stat-card tone-${tone}`}>
      <div className="stat-card-main">
        {IconComp && (
          <span className={`icon-badge tone-${tone}`}>
            <IconComp width={20} height={20} />
          </span>
        )}
        <div>
          <strong>{value ?? 0}</strong>
          <span>{label}</span>
        </div>
      </div>
      {breakdown && breakdown.length > 0 && (
        <div className="stat-breakdown">
          {breakdown.map(([bLabel, bValue]) => (
            <div key={bLabel}>
              <strong>{bValue}</strong>
              <span>{bLabel}</span>
            </div>
          ))}
        </div>
      )}
    </article>
  );
}
