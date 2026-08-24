import { useState } from 'react'
import Card from '../ui/Card'

const NODE_EXPORTER_VERSION = '1.11.1'

type Arch = 'amd64' | 'arm64'

function buildScript(arch: Arch) {
  const archiveName = `node_exporter-${NODE_EXPORTER_VERSION}.linux-${arch}`
  return `cd /tmp
wget https://github.com/prometheus/node_exporter/releases/download/v${NODE_EXPORTER_VERSION}/${archiveName}.tar.gz
tar xvfz ${archiveName}.tar.gz
sudo mv ${archiveName}/node_exporter /usr/local/bin/
rm -rf ${archiveName} ${archiveName}.tar.gz

sudo useradd --no-create-home --shell /bin/false node_exporter 2>/dev/null || true
sudo chown node_exporter:node_exporter /usr/local/bin/node_exporter

sudo tee /etc/systemd/system/node_exporter.service > /dev/null <<'EOF'
[Unit]
Description=Node Exporter
After=network.target

[Service]
User=node_exporter
Group=node_exporter
Type=simple
ExecStart=/usr/local/bin/node_exporter --web.listen-address=127.0.0.1:9100

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable node_exporter
sudo systemctl start node_exporter
sudo systemctl status node_exporter --no-pager`
}

function CopyableSnippet({ code, label }: { code: string; label?: string }) {
  const [copied, setCopied] = useState(false)

  const handleCopy = async () => {
    await navigator.clipboard.writeText(code)
    setCopied(true)
    setTimeout(() => setCopied(false), 1500)
  }

  return (
    <div className="relative group mt-1.5">
      <button
        type="button"
        onClick={handleCopy}
        className="absolute top-2.5 right-2.5 z-10 inline-flex items-center gap-1.5 rounded-md bg-slate-800/90 hover:bg-slate-700 border border-slate-700 px-2 py-1 text-[11px] font-semibold text-white shadow-2xs transition-all"
        title="복사"
      >
        {copied ? (
          <>
            <svg className="h-3 w-3 text-emerald-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
            </svg>
            <span className="text-emerald-300 text-[10px]">복사됨</span>
          </>
        ) : (
          <>
            <svg className="h-3 w-3 text-slate-300" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
            </svg>
            <span className="text-[10px]">{label || '복사'}</span>
          </>
        )}
      </button>
      <pre className="overflow-x-auto rounded-lg bg-slate-900 p-3.5 pr-20 font-mono text-xs leading-relaxed whitespace-pre text-slate-200 shadow-inner border border-slate-800">
        {code}
      </pre>
    </div>
  )
}

interface NodeExporterSetupCardProps {
  targetInstance?: { name: string; ip: string } | null
}

export default function NodeExporterSetupCard({ targetInstance }: NodeExporterSetupCardProps) {
  const [arch, setArch] = useState<Arch>('amd64')
  const [copied, setCopied] = useState(false)
  const script = buildScript(arch)

  const handleCopy = async () => {
    await navigator.clipboard.writeText(script)
    setCopied(true)
    setTimeout(() => setCopied(false), 1500)
  }

  return (
    <Card className="p-6">
      {/* 카드 상단 헤더 & 대상 인스턴스 뱃지 */}
      <div className="mb-4 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2.5 pb-3 border-b border-slate-100">
        <div>
          <h2 className="text-base font-bold text-slate-900">2. 대상 EC2에서 Node Exporter 설치</h2>
          <p className="text-xs text-slate-500 mt-0.5">최초 1회 · 대상 호스트 SSH 터미널에 스크립트를 붙여넣어 실행하세요.</p>
        </div>

        {targetInstance && (
          <div className="inline-flex items-center gap-1.5 rounded-lg bg-brand-50 border border-brand-200 px-3 py-1 text-xs text-brand-900 self-start sm:self-auto">
            <span className="font-semibold text-brand-700">대상 인스턴스:</span>
            <span className="font-bold text-slate-900">{targetInstance.name}</span>
            <span className="font-mono text-brand-800 font-medium">({targetInstance.ip})</span>
          </div>
        )}
      </div>

      {/* 아키텍처 선택 */}
      <div className="mb-3 flex flex-wrap items-center gap-2 text-xs">
        <span className="font-semibold text-slate-600">EC2 아키텍처:</span>
        <div className="flex rounded-md border border-slate-200 p-0.5 bg-slate-100">
          <button
            type="button"
            onClick={() => setArch('amd64')}
            className={`rounded px-2.5 py-1 font-semibold transition-colors ${
              arch === 'amd64' ? 'bg-white text-brand-700 shadow-2xs' : 'text-slate-600'
            }`}
          >
            x86_64 (amd64)
          </button>
          <button
            type="button"
            onClick={() => setArch('arm64')}
            className={`rounded px-2.5 py-1 font-semibold transition-colors ${
              arch === 'arm64' ? 'bg-white text-brand-700 shadow-2xs' : 'text-slate-600'
            }`}
          >
            arm64 (Graviton)
          </button>
        </div>
      </div>

      {/* 메인 설치 스크립트 코드 블록 & 우측 상단 인라인 복사 버튼 */}
      <div className="relative group">
        <button
          type="button"
          onClick={handleCopy}
          className="absolute top-3 right-3 z-10 inline-flex items-center gap-1.5 rounded-md bg-slate-800/90 hover:bg-slate-800 border border-slate-700 px-2.5 py-1.5 text-xs font-semibold text-white shadow-sm transition-all"
          title="스크립트 복사"
        >
          {copied ? (
            <>
              <svg className="h-3.5 w-3.5 text-emerald-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2.5}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
              </svg>
              <span className="text-emerald-300">복사됨</span>
            </>
          ) : (
            <>
              <svg className="h-3.5 w-3.5 text-slate-300" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
              </svg>
              <span>스크립트 복사</span>
            </>
          )}
        </button>

        <pre className="overflow-x-auto rounded-lg bg-slate-900 p-4 pt-4 pr-32 font-mono text-xs leading-relaxed whitespace-pre text-slate-200 shadow-inner border border-slate-800">
          {script}
        </pre>
      </div>

      <p className="mt-3 rounded-lg border border-brand-200 bg-brand-50/50 px-3.5 py-2.5 text-xs text-brand-900 leading-relaxed">
        <strong>보안 안내</strong>: Node Exporter는 <code className="bg-white px-1 py-0.5 rounded border border-brand-200 font-mono font-bold text-brand-800">127.0.0.1:9100</code> 에만 로컬 바인딩되므로 외부 보안그룹(Inbound Security Group)을 변경하지 않아도 안전하게 동작합니다.
      </p>

      {/* Docker FAQ 드롭다운 */}
      <details className="mt-4 rounded-lg border border-slate-200 px-4 py-3 text-xs text-slate-600 bg-slate-50/50">
        <summary className="cursor-pointer font-bold text-slate-800">
          FAQ · 앱을 Docker로 띄우는데 인스턴스 지표가 계속 수집되지 않아요
        </summary>
        <div className="mt-3 space-y-3 leading-relaxed text-xs">
          <p>
            앱이 도커 컨테이너 안에서 실행 중이라면, 컨테이너의 <code className="rounded bg-white px-1 py-0.5 border border-slate-200 font-mono text-slate-700">localhost</code>
            는 EC2 호스트가 아니라 컨테이너 자기 자신을 가리킵니다. 위 스크립트는 Node Exporter를{' '}
            <code className="rounded bg-white px-1 py-0.5 border border-slate-200 font-mono text-slate-700">127.0.0.1:9100</code>에만 바인딩하므로, 호스트 밖(도커
            브리지 네트워크)에서 오는 연결은 커널 단에서부터 거부되어 라이브러리가 host 메트릭을 계속 비워서 보내게 됩니다.
          </p>

          <p className="font-bold text-slate-800 pt-1">해결 순서</p>
          <div className="space-y-3">
            <div>
              <span className="font-semibold text-slate-700">1. EC2 호스트에서 바인딩 주소를 0.0.0.0으로 넓힙니다:</span>
              <CopyableSnippet
                code={`sudo sed -i 's/--web.listen-address=127.0.0.1:9100/--web.listen-address=0.0.0.0:9100/' /etc/systemd/system/node_exporter.service
sudo systemctl daemon-reload
sudo systemctl restart node_exporter`}
              />
            </div>

            <div>
              <span className="font-semibold text-slate-700">2. docker-compose.yml 앱 서비스에 호스트 접근 경로를 추가합니다:</span>
              <CopyableSnippet
                code={`services:
  app:
    extra_hosts:
      - "host.docker.internal:host-gateway"
    environment:
      MONI_NODE_EXPORTER_URL: http://host.docker.internal:9100/metrics`}
              />
            </div>

            <div>
              <span className="font-semibold text-slate-700">3. 컨테이너를 재생성합니다:</span>
              <CopyableSnippet code="docker compose up -d --force-recreate app" />
            </div>
          </div>

          <p className="pt-2 text-slate-500">
            확인은 컨테이너 안에서 <code className="rounded bg-white px-1 py-0.5 border border-slate-200 font-mono text-slate-700">curl http://host.docker.internal:9100/metrics</code>
            가 응답하는지로 하면 됩니다. 0.0.0.0으로 넓힌 뒤에는 EC2 보안그룹에서 9100 포트를 외부에 열지 않았는지 함께 확인하세요.
          </p>
        </div>
      </details>
    </Card>
  )
}