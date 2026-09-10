import fs from 'node:fs';
import path from 'node:path';
import { spawn, execSync } from 'node:child_process';
import readline from 'node:readline';

const cwd = 'C:\\Dev\\Pmsg';

// 1. Limpeza proativa de arquivo de PID órfão da aplicação Desktop
try {
  const pidFilePath = path.join(cwd, 'composeApp', 'build', 'run', 'desktopMain', 'desktopMain.pid');
  if (fs.existsSync(pidFilePath)) {
    const content = fs.readFileSync(pidFilePath, 'utf8');
    const pidMatch = content.match(/pid=(\d+)/);
    if (pidMatch) {
      const targetPid = parseInt(pidMatch[1], 10);
      let isAlive = false;
      try {
        process.kill(targetPid, 0);
        isAlive = true;
      } catch {
        isAlive = false;
      }
      if (!isAlive) {
        fs.unlinkSync(pidFilePath);
        process.stderr.write(`[mcp-stdio-filter] Arquivo de PID órfão removido (PID ${targetPid} não está em execução).\n`);
      }
    }
  }
} catch {
  // Ignora falhas não-críticas na inspeção de PID prévio
}

// 2. Inicialização do processo filho com gradlew
const child = spawn(
  'cmd.exe',
  [
    '/c',
    'gradlew.bat',
    '--no-daemon',
    '--quiet',
    '--console=plain',
    ':composeApp:hotMcpServerDesktop'
  ],
  {
    cwd,
    stdio: ['pipe', 'pipe', 'pipe'],
    windowsHide: true,
  }
);

let isTerminating = false;

function shutdown(exitCode = 0) {
  if (isTerminating) return;
  isTerminating = true;

  if (child && child.pid) {
    try {
      if (process.platform === 'win32') {
        // Encerra toda a árvore de processos (cmd.exe -> gradlew.bat -> java.exe)
        execSync(`taskkill /pid ${child.pid} /T /F`, { stdio: 'ignore' });
      } else {
        child.kill('SIGTERM');
      }
    } catch {
      try {
        child.kill();
      } catch {
        // Processo já finalizado
      }
    }
  }

  process.exit(exitCode);
}

// Trata encerramento do canal de entrada stdin pelo cliente MCP (IDE)
process.stdin.on('end', () => shutdown(0));
process.stdin.on('close', () => shutdown(0));

// Trata sinais do sistema operacional
process.on('SIGINT', () => shutdown(0));
process.on('SIGTERM', () => shutdown(0));
process.on('SIGHUP', () => shutdown(0));

// Ignora EPIPE caso o stdin do filho feche prematuramente
child.stdin.on('error', (err) => {
  if (err.code !== 'EPIPE') {
    process.stderr.write(`[mcp-stdio-filter] Erro de escrita no stdin: ${err.message}\n`);
  }
});

// Encaminha stderr do filho diretamente para stderr do pai (logs permitidos pelo protocolo MCP)
child.stderr.pipe(process.stderr);

// Encaminha stdin do pai (da IDE) para o stdin do filho (JSON-RPC requests)
process.stdin.pipe(child.stdin);

// Filtra o stdout linha a linha, garantindo isolamento estrito de JSON-RPC
const rl = readline.createInterface({
  input: child.stdout,
  crlfDelay: Infinity,
});

rl.on('line', (line) => {
  const trimmed = line.trim();
  if (trimmed.startsWith('{')) {
    // Linha JSON-RPC esperada pela IDE
    process.stdout.write(line + '\n');
  } else if (trimmed.length > 0) {
    // Linha de ruído (Gradle, SLF4J, etc) redirecionada para stderr
    process.stderr.write(line + '\n');
  }
});

child.on('error', (err) => {
  process.stderr.write(`[mcp-stdio-filter] Erro no processo filho: ${err.message}\n`);
  shutdown(1);
});

child.on('exit', (code, signal) => {
  if (isTerminating) {
    process.exit(0);
    return;
  }
  // Se o código for null (finalizado por sinal/taskkill), encerra com código de sucesso 0
  if (code !== null) {
    process.exit(code);
  } else {
    process.exit(0);
  }
});
