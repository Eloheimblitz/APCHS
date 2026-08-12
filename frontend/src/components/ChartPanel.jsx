import { useMemo, useState } from 'react';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  LabelList,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis
} from 'recharts';

const palette = ['#2563eb', '#0f766e', '#d97706', '#dc2626', '#7c3aed', '#0891b2', '#65a30d', '#c2410c'];

const namedColors = {
  YES: '#2563eb',
  NO: '#94a3b8',
  TRUE: '#2563eb',
  FALSE: '#94a3b8',
  GAS: '#2563eb',
  WOOD: '#c2410c',
  COAL: '#475569',
  ELECTRICITY: '#0891b2'
};

export default function ChartPanel({ title, data = {}, type = 'bar' }) {
  const [activeIndex, setActiveIndex] = useState(null);
  const rows = useMemo(() => Object.entries(data || {})
    .filter(([, value]) => Number(value) > 0)
    .map(([name, value], index) => ({
      key: name,
      name: formatName(name),
      shortName: compactName(name),
      value: Number(value),
      color: colorFor(name, index)
    })), [data]);

  const total = rows.reduce((sum, row) => sum + row.value, 0);

  return (
    <section className="chart-card interactive-chart">
      <div className="chart-card-header">
        <div>
          <h2>{title}</h2>
          <span>{total} total</span>
        </div>
      </div>

      {rows.length === 0 ? (
        <div className="chart-empty">
          <strong>No responses yet</strong>
          <span>This chart will update as surveys are submitted.</span>
        </div>
      ) : type === 'pie' ? (
        <PieChartView rows={rows} total={total} activeIndex={activeIndex} setActiveIndex={setActiveIndex} />
      ) : (
        <BarChartView rows={rows} total={total} activeIndex={activeIndex} setActiveIndex={setActiveIndex} />
      )}
    </section>
  );
}

function BarChartView({ rows, total, activeIndex, setActiveIndex }) {
  return (
    <>
      <div className="chart-viewport">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={rows} margin={{ top: 20, right: 8, left: -18, bottom: 8 }} onMouseLeave={() => setActiveIndex(null)}>
            <CartesianGrid stroke="#edf2f7" strokeDasharray="3 5" vertical={false} />
            <XAxis
              axisLine={false}
              dataKey="shortName"
              interval={0}
              tick={{ fill: '#64748b', fontSize: 11, fontWeight: 800 }}
              tickLine={false}
            />
            <YAxis
              allowDecimals={false}
              axisLine={false}
              tick={{ fill: '#94a3b8', fontSize: 11, fontWeight: 800 }}
              tickLine={false}
              width={36}
            />
            <Tooltip content={<ChartTooltip total={total} />} cursor={{ fill: 'rgba(37, 99, 235, 0.05)' }} />
            <Bar dataKey="value" radius={[7, 7, 0, 0]} maxBarSize={48} onMouseEnter={(_, index) => setActiveIndex(index)}>
              {rows.map((row, index) => (
                <Cell
                  key={row.key}
                  fill={row.color}
                  opacity={activeIndex === null || activeIndex === index ? 1 : 0.42}
                />
              ))}
              <LabelList dataKey="value" position="top" fill="#334155" fontSize={11} fontWeight={900} />
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div>
      <ChartLegend rows={rows} activeIndex={activeIndex} setActiveIndex={setActiveIndex} />
    </>
  );
}

function PieChartView({ rows, total, activeIndex, setActiveIndex }) {
  const activeRow = activeIndex === null ? null : rows[activeIndex];

  return (
    <>
      <div className="chart-viewport donut-viewport">
        <ResponsiveContainer width="100%" height="100%">
          <PieChart onMouseLeave={() => setActiveIndex(null)}>
            <Pie
              data={rows}
              dataKey="value"
              innerRadius="58%"
              isAnimationActive
              label={false}
              labelLine={false}
              nameKey="name"
              outerRadius="84%"
              paddingAngle={3}
              onMouseEnter={(_, index) => setActiveIndex(index)}
            >
              {rows.map((row, index) => (
                <Cell
                  key={row.key}
                  fill={row.color}
                  opacity={activeIndex === null || activeIndex === index ? 1 : 0.42}
                  stroke="#ffffff"
                  strokeWidth={3}
                />
              ))}
            </Pie>
            <Tooltip content={<ChartTooltip total={total} />} />
          </PieChart>
        </ResponsiveContainer>
        <div className="donut-center">
          <strong>{activeRow ? activeRow.value : total}</strong>
          <span>{activeRow ? activeRow.name : 'Total'}</span>
        </div>
      </div>
      <ChartLegend rows={rows} activeIndex={activeIndex} setActiveIndex={setActiveIndex} />
    </>
  );
}

function ChartLegend({ rows, activeIndex, setActiveIndex }) {
  return (
    <div className="chart-legend">
      {rows.map((row, index) => {
        const percent = rows.reduce((sum, item) => sum + item.value, 0)
          ? Math.round((row.value / rows.reduce((sum, item) => sum + item.value, 0)) * 100)
          : 0;
        return (
          <button
            className={activeIndex === index ? 'active' : ''}
            key={row.key}
            onMouseEnter={() => setActiveIndex(index)}
            onMouseLeave={() => setActiveIndex(null)}
            type="button"
          >
            <i style={{ background: row.color }} />
            <span>{row.name}</span>
            <strong>{row.value}</strong>
            <small>{percent}%</small>
          </button>
        );
      })}
    </div>
  );
}

function ChartTooltip({ active, payload, total }) {
  if (!active || !payload?.length) return null;
  const item = payload[0].payload || payload[0];
  const value = Number(item.value || 0);
  const percent = total ? Math.round((value / total) * 100) : 0;

  return (
    <div className="chart-tooltip">
      <strong>{item.name}</strong>
      <span>{value} responses</span>
      <small>{percent}% of total</small>
    </div>
  );
}

function colorFor(value, index) {
  const normalized = String(value).toUpperCase();
  return namedColors[normalized] || palette[index % palette.length];
}

function compactName(value) {
  const label = formatName(value);
  return label.length > 12 ? `${label.slice(0, 11)}...` : label;
}

function formatName(value) {
  const normalized = String(value);
  if (normalized === 'true') return 'Yes';
  if (normalized === 'false') return 'No';
  return normalized
    .replace(/([a-z])([A-Z])/g, '$1 $2')
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (letter) => letter.toUpperCase())
    .replace(/\bBp\b/g, 'BP');
}
