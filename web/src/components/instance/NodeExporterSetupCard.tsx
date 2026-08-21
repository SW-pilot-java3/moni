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

export default function NodeExporterSetupCard() {
  const [arch, setArch] = useState<Arch>('amd64')
  const [copied, setCopied] = useState(false)
  const script = buildScript(arch)

  const handleCopy = async () => {
    await navigator.clipboard.writeText(script)
    setCopied(true)
    setTimeout(() => setCopied(false), 1500)
  }

  return (
    <Card className="mb-6 p-6">
      <div className="mb-4 flex items-center justify-between">
        <h2 className="font-semibold text-slate-900">② 대상 EC2에서 Node Exporter 설치</h2>
        <span className="text-xs text-slate-400">최초 1회 · SSH 터미널에서 실행</span>
      </div>

      <div className="mb-3 flex items-center gap-2 text-xs">
        <span className="text-slate-500">EC2 아키텍처</span>
        <div className="flex rounded-md border border-slate-200 p-0.5">
          <button
            type="button"
            onClick={() => setArch('amd64')}
            className={`rounded px-3 py-1 font-medium ${arch === 'amd64' ? 'bg-brand-500 text-white' : 'text-slate-500'}`}
          >
            x86_64 (amd64)
          </button>
          <button
            type="button"
            onClick={() => setArch('arm64')}
            className={`rounded px-3 py-1 font-medium ${arch === 'arm64' ? 'bg-brand-500 text-white' : 'text-slate-500'}`}
          >
            arm64 (Graviton)
          </button>
        </div>
        <span className="text-slate-400">— 모르면 EC2에서 `uname -m` 실행: x86_64 → amd64, aarch64 → arm64</span>
      </div>

      <pre className="overflow-x-auto rounded-md bg-slate-50 p-4 font-mono text-xs leading-relaxed whitespace-pre text-slate-700">
        {script}
      </pre>

      <div className="mt-3 flex items-center gap-3">
        <button
          type="button"
          onClick={handleCopy}
          className="rounded-md border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
        >
          {copied ? '복사됨' : '스크립트 복사'}
        </button>
        <p className="text-xs text-slate-400">전체를 그대로 EC2 SSH 터미널에 붙여넣으면 설치부터 서비스 등록까지 완료됩니다.</p>
      </div>

      <p className="mt-3 rounded-md border border-warn-500/40 bg-warn-50 px-3 py-2.5 text-sm text-warn-600">
        Node Exporter는 127.0.0.1:9100 에만 바인딩되어 외부에 노출되지 않으므로 보안그룹은 변경하지 않아도 됩니다.
      </p>

      <details className="mt-4 rounded-md border border-slate-200 px-4 py-3 text-sm text-slate-600">
        <summary className="cursor-pointer font-medium text-slate-700">
          FAQ · 앱을 Docker로 띄우는데 인스턴스 지표가 계속 수집되지 않아요
        </summary>
        <div className="mt-3 space-y-2 leading-relaxed">
          <p>
            앱이 도커 컨테이너 안에서 실행 중이라면, 컨테이너의 <code className="rounded bg-slate-100 px-1 py-0.5">localhost</code>
            는 EC2 호스트가 아니라 컨테이너 자기 자신을 가리킵니다. 위 스크립트는 Node Exporter를{' '}
            <code className="rounded bg-slate-100 px-1 py-0.5">127.0.0.1:9100</code>에만 바인딩하므로, 호스트 밖(도커
            브리지 네트워크)에서 오는 연결은 커널 단에서부터 거부되어 라이브러리가 host 메트릭을 계속 비워서 보내게 됩니다.
          </p>
          <p className="font-medium text-slate-700">해결 순서</p>
          <ol className="list-decimal space-y-1 pl-5">
            <li>
              EC2 호스트에서 바인딩 주소를 넓힙니다.
              <pre className="mt-1 overflow-x-auto rounded-md bg-slate-50 p-3 font-mono text-xs text-slate-700">
{`sudo sed -i 's/--web.listen-address=127.0.0.1:9100/--web.listen-address=0.0.0.0:9100/' /etc/systemd/system/node_exporter.service
sudo systemctl daemon-reload
sudo systemctl restart node_exporter`}
              </pre>
            </li>
            <li>
              <code className="rounded bg-slate-100 px-1 py-0.5">docker-compose.yml</code>의 앱 서비스에 호스트 접근 경로를
              추가합니다.
              <pre className="mt-1 overflow-x-auto rounded-md bg-slate-50 p-3 font-mono text-xs text-slate-700">
{`services:
  app:
    extra_hosts:
      - "host.docker.internal:host-gateway"
    environment:
      MONI_NODE_EXPORTER_URL: http://host.docker.internal:9100/metrics`}
              </pre>
            </li>
            <li>
              컨테이너를 재생성합니다.
              <pre className="mt-1 overflow-x-auto rounded-md bg-slate-50 p-3 font-mono text-xs text-slate-700">
{`docker compose up -d --force-recreate app`}
              </pre>
            </li>
          </ol>
          <p>
            확인은 컨테이너 안에서 <code className="rounded bg-slate-100 px-1 py-0.5">curl http://host.docker.internal:9100/metrics</code>
            가 응답하는지로 하면 됩니다. 0.0.0.0으로 넓힌 뒤에는 EC2 보안그룹에서 9100 포트를 외부(0.0.0.0/0)에 열지
            않았는지 함께 확인하세요 — 보안그룹은 인스턴스 안(로컬/도커 브리지) 통신에는 관여하지 않으므로 추가로 열
            필요는 없습니다.
          </p>
        </div>
      </details>
    </Card>
  )
}