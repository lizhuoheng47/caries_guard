FROM python:3.11-slim

WORKDIR /app
ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1
ENV PYTHONPATH=/app

COPY requirements.txt ./
# 缓存 PyTorch/CUDA 等大体积依赖，构建中断后可复用且不会增大最终镜像。
RUN --mount=type=cache,target=/root/.cache/pip \
    pip install -r requirements.txt

# Ultralytics 会加载 OpenCV，其本地扩展需要这两个精简镜像未预装的运行库。
RUN sed -i 's|http://deb.debian.org|https://deb.debian.org|g' /etc/apt/sources.list.d/debian.sources \
    && apt-get update \
    && apt-get install -y --no-install-recommends libgl1 libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

COPY app ./app
COPY assets ./assets
COPY knowledge-base ./knowledge-base
COPY tests ./tests
COPY alembic.ini ./
COPY migrations ./migrations
COPY scripts ./scripts
RUN chmod +x scripts/entrypoint.sh

EXPOSE 8001

CMD ["scripts/entrypoint.sh"]
