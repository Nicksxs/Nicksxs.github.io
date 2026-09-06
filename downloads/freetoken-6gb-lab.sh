#!/usr/bin/env bash
set -euo pipefail

MODEL_REPO="${FT_MODEL_REPO:-nvidia/Qwen3.6-35B-A3B-NVFP4}"
MODEL_DIR="${FT_MODEL_DIR:-$HOME/models/Qwen3.6-35B-A3B-NVFP4}"
MODEL="${FT_MODEL:-$MODEL_REPO}"
GPU="${FT_GPU:-0}"
PORT="${FT_PORT:-1919}"
BASE_URL="${FT_BASE_URL:-http://127.0.0.1:${PORT}}"
MEMORY_RATIO="${FT_MEMORY_RATIO:-0.92}"
MOE_CACHE_SIZE="${FT_MOE_CACHE_SIZE:-512}"
MAX_PREFILL="${FT_MAX_PREFILL:-2048}"
MAX_REQUESTS="${FT_MAX_REQUESTS:-1}"
MAX_OUTPUT="${FT_MAX_OUTPUT:-2048}"
LOG_DIR="${FT_LOG_DIR:-./freetoken-6gb-logs}"

usage() {
  cat <<'EOF'
Usage: freetoken-6gb-lab.sh <command>

Commands:
  preflight  Inspect GPU, CUDA, RAM, swap, CPU and disk
  download   Download the supported NVFP4 safetensors checkpoint
  verify-model  Verify the local checkpoint has all three weight shards
  bench      Benchmark NVFP4 CPU/PCIe bandwidth
  run        Start FreeToken with the conservative 6GB profile
  status     Save health/cache/stats snapshots
  smoke      Generate 128 tokens as a basic serving test
  sweep      Try expert cache sizes 512, 640, 768, 896 and 940

Environment overrides:
  FT_MODEL_REPO FT_MODEL_DIR FT_MODEL FT_GPU FT_PORT
  FT_MEMORY_RATIO FT_MOE_CACHE_SIZE
  FT_MAX_PREFILL FT_MAX_REQUESTS FT_MAX_OUTPUT FT_LOG_DIR
EOF
}

need() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "ERROR: missing command: $1" >&2
    exit 1
  }
}

download_model() {
  need hf
  mkdir -p "$MODEL_DIR"

  echo "Downloading repository: $MODEL_REPO"
  echo "Destination: $MODEL_DIR"
  hf download "$MODEL_REPO" --local-dir "$MODEL_DIR"
  verify_model
}

verify_model() {
  local missing=0
  local required=(
    config.json
    model.safetensors.index.json
    model-00001-of-00003.safetensors
    model-00002-of-00003.safetensors
    model-00003-of-00003.safetensors
  )

  echo "Checking model directory: $MODEL_DIR"
  local file
  for file in "${required[@]}"; do
    if [[ -s "$MODEL_DIR/$file" ]]; then
      printf '  OK      %s\n' "$file"
    else
      printf '  MISSING %s\n' "$file" >&2
      missing=1
    fi
  done

  if (( missing != 0 )); then
    echo "ERROR: checkpoint is incomplete or is not the supported NVFP4 safetensors repository." >&2
    exit 1
  fi

  du -sh "$MODEL_DIR"
}

timestamp() {
  date '+%Y%m%d-%H%M%S'
}

preflight() {
  need nvidia-smi
  need nvcc
  need python3

  echo '== GPU =='
  nvidia-smi --query-gpu=name,memory.total,memory.used,memory.free,driver_version,pci.bus_id \
    --format=csv,noheader
  echo
  nvidia-smi

  echo
  echo '== CUDA =='
  nvcc --version

  echo
  echo '== Python =='
  python3 --version

  echo
  echo '== RAM and swap =='
  free -h
  swapon --show || true

  echo
  echo '== CPU =='
  lscpu | grep -E 'Model name|Socket|Core|Thread|Flags' || lscpu

  echo
  echo '== Model cache disk =='
  local cache_dir="${HF_HOME:-$HOME/.cache/huggingface}"
  mkdir -p "$cache_dir"
  df -h "$cache_dir"

  echo
  echo 'Check manually:'
  echo '  - NVIDIA Ampere/RTX 30 series or newer'
  echo '  - driver r580+ and CUDA 13 toolkit'
  echo '  - close other GPU processes before serving'
  echo '  - about 30 GB free RAM for the 35B NVFP4 experiment'
}

bench() {
  need ft
  ft bench bw --dtype nvfp4 --gpu "$GPU"
}

run_server() {
  need ft
  export PYTORCH_CUDA_ALLOC_CONF="${PYTORCH_CUDA_ALLOC_CONF:-expandable_segments:True}"

  echo "Starting model: $MODEL"
  echo "GPU=$GPU memory_ratio=$MEMORY_RATIO moe_cache=$MOE_CACHE_SIZE max_prefill=$MAX_PREFILL"

  exec ft serve \
    --model "$MODEL" \
    --gpu "$GPU" \
    --host 127.0.0.1 \
    --port "$PORT" \
    --attn triton \
    --moe-backend auto \
    --memory-ratio "$MEMORY_RATIO" \
    --max-running-requests "$MAX_REQUESTS" \
    --max-prefill-length "$MAX_PREFILL" \
    --moe-cache-size "$MOE_CACHE_SIZE" \
    --max-output-tokens "$MAX_OUTPUT" \
    --decode-log-interval 20
}

status() {
  need ft
  mkdir -p "$LOG_DIR"
  local stamp
  stamp="$(timestamp)"

  ft ctl --base-url "$BASE_URL" health | tee "$LOG_DIR/${stamp}-health.txt"
  ft ctl --base-url "$BASE_URL" --json cache | tee "$LOG_DIR/${stamp}-cache.json"
  ft ctl --base-url "$BASE_URL" --json stats | tee "$LOG_DIR/${stamp}-stats.json"
  nvidia-smi | tee "$LOG_DIR/${stamp}-nvidia-smi.txt"
}

smoke() {
  need ft
  ft ctl --base-url "$BASE_URL" generate \
    '用三句话解释MoE中总参数和激活参数的区别' \
    --max-tokens 128
}

sweep() {
  need ft
  mkdir -p "$LOG_DIR"

  echo 'WARNING: this command rebuilds the live cache pools.'
  echo 'It stops on the first failed size. Keep another terminal open with nvidia-smi.'

  local size stamp
  for size in 512 640 768 896 940; do
    echo
    echo "== expert slots: $size =="
    ft ctl --base-url "$BASE_URL" cache --moe "$size" --wait 300

    stamp="$(timestamp)-moe-${size}"
    ft ctl --base-url "$BASE_URL" --json cache | tee "$LOG_DIR/${stamp}-cache.json"

    for round in 1 2 3; do
      echo "-- round $round --"
      ft ctl --base-url "$BASE_URL" generate \
        '实现一个线程安全的LRU缓存并解释锁粒度' \
        --max-tokens 256 \
        --ignore-eos | tee "$LOG_DIR/${stamp}-round-${round}.txt"
    done

    ft ctl --base-url "$BASE_URL" --json stats | tee "$LOG_DIR/${stamp}-stats.json"
    ft ctl --base-url "$BASE_URL" --json requests --limit 20 | tee "$LOG_DIR/${stamp}-requests.json"
  done
}

case "${1:-}" in
  preflight) preflight ;;
  download) download_model ;;
  verify-model) verify_model ;;
  bench) bench ;;
  run) run_server ;;
  status) status ;;
  smoke) smoke ;;
  sweep) sweep ;;
  *) usage; exit 1 ;;
esac
