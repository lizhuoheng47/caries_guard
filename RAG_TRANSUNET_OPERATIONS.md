# CariesGuard RAG 与 TransUNet 操作说明

## 1. 两者不是同一种技术

TransUNet 是医学影像分割网络：输入影像，输出像素级掩膜。它由 CNN/U-Net 路径与 Transformer 视觉编码器组成，不读取知识库，也不属于 RAG。

RAG 是检索增强生成：输入结构化病灶或问题，从审核过的文档中检索证据，再让语言模型或模板依据证据生成解释。它不能替代影像模型定位病灶，也不能修改检测框、分割掩膜或疾病类别。

```text
影像 -> 检测/分割模型（YOLO、TransUNet）-> 结构化结果
                                            |
审核文档 -> 切分/向量索引 -> RAG 检索 --------+-> 带引用的解释
```

## 2. 第一版 RAG 操作流程

第一版不需要训练 RAG。使用预训练 embedding 模型或低成本 embedding API 建索引即可；只有检索评测长期不达标时，才考虑微调 embedding 或 reranker。

### 2.1 准备资料

优先级从高到低：

1. ICDAS/ICCMS 官方定义和放射学评分资料。
2. 国家或专业学会发布的龋病诊疗指南。
3. 由指导教师审核的本地诊疗流程和患者宣教材料。

每份资料记录 `documentCode`、标题、版本、发布日期、来源 URL、适用人群、审核人、审核日期和许可说明。未经审核的网页、论坛内容和模型生成文本不得发布到正式知识库。

### 2.2 本地目录

```text
data/knowledge/
  source/       # 原始 PDF/DOCX/网页快照，不提交含版权或隐私材料
  normalized/   # 清洗后的 UTF-8 Markdown/JSON
  eval/         # 问题、期望引用、拒答样例
```

### 2.3 解析和切分

1. 提取标题、章节、正文和表格；扫描 PDF 先 OCR，再人工抽查。
2. 按语义章节切分，建议每块约 300-600 个中文字符，重叠 50-100 字。
3. 每块保留文档编号、版本、章节、页码、来源 URL 和审核状态。
4. 只将 `REVIEWED + PUBLISHED` 的块加入可检索索引。

### 2.4 建索引

开发阶段使用仓库已有的 `LOCAL_JSON`，避免部署 OpenSearch/Neo4j。验证流程后再决定是否升级。

建议开发配置：

```dotenv
CG_RAG_RUNTIME_ENABLED=true
CG_RAG_VECTOR_STORE_TYPE=LOCAL_JSON
CG_RAG_INDEX_DIR=./data/rag-index
CG_RAG_DEFAULT_KB_CODE=caries-first-version
CG_RAG_KNOWLEDGE_VERSION=v1.0-reviewed
CG_ANALYSIS_KB_ENHANCEMENT_ENABLED=false
```

知识库尚未完成审核和评测前，`CG_ANALYSIS_KB_ENHANCEMENT_ENABLED` 必须保持 `false`。正式语义检索需要接入本地 embedding provider 或已配置密钥的 OpenAI-compatible provider；`HASHING` 只能用于链路开发，不能作为正式医学语义检索。

### 2.5 评测和发布

准备至少三组问题：

- 20-30 个应当命中指定指南章节的问题。
- 10-20 个证据不足、必须拒答的问题。
- 10 个提示注入或要求绕过医生复核的问题。

验收指标至少包括 Recall@5、期望引用覆盖率、无证据拒答率、错误引用率和人工一致性。通过后再把分析结果中的疾病类别、牙位和严重度作为检索查询，回填 `knowledgeVersion`、`evidenceRefs` 和 citations。

当前仓库已经有知识服务、切分、索引和评测服务，但尚未注册知识导入/RAG HTTP 路由，前端也没有真实知识管理页面；这些属于 P4，不能仅靠修改环境变量宣称已经可用。

## 3. TransUNet 是否需要权重

需要区分两类权重：

1. **初始化权重**：例如 ImageNet-21k 的 `R50+ViT-B_16.npz`。不是绝对必需，但小型医学数据从随机参数训练通常效果差，强烈建议使用。
2. **你的任务权重**：使用牙片和人工像素掩膜训练得到的 `best.pt`/`best.pth`，这是实际推理必须使用的权重。

官方 TransUNet 仓库在 2026-02 提示原 ViT 下载链接已经失效，并在其项目文件夹提供副本。下载后必须记录来源和 SHA-256，不应将未知网盘权重直接用于项目。

## 4. 在 RTX 4060 8GB 上训练 TransUNet

DENTEX 主要提供检测框/层级检测标注，不能直接训练可信的病灶分割。把矩形框内部全部填成掩膜会制造错误标签。但不必从零手工描绘整套 mask：可以使用公开的真实分割标注做基线，再用提示式分割模型降低少量本地复核的成本。

### 4.1 没有自建 mask 时的数据路线

1. 优先使用 [DC1000 龋病分割数据及官方代码](https://github.com/Zzz512/MLUA) 建立公开数据基线。它是全景片龋病病灶分割数据，包含标注与未标注样本；下载后必须再核对影像-mask 映射、原始划分和许可条款。
2. [Children's dental panoramic radiographs dataset](https://www.nature.com/articles/s41597-023-02237-5) 中的像素 mask 主要是牙齿结构，疾病标注主要是检测 JSON；可用于牙齿/ROI 分割，不能把它当成龋病边界真值。
3. DENTEX 继续用于龋病/深龋等异常检测，不与分割标签混用。
4. 如有少量自有影像，用 [MedSAM](https://github.com/bowang-lab/MedSAM) 或 LiteMedSAM 接收检测框/点提示，自动生成候选 mask。
5. 对候选 mask 执行 `ACCEPT / CORRECT / REJECT` 复核。已通过人工复核的才进入金标集；未复核的只能标记为 `PSEUDO`。
6. 如果完全无法得到专业复核，可以完成“公开数据学术原型”，但不得宣称对本地成人、不同设备或临床人群已可靠。

推荐目录：

```text
data/segmentation/
  public/images/       # 公开原始影像
  public/masks/        # 公开真值 mask
  pseudo/images/       # 待复核本地影像
  pseudo/masks/        # MedSAM 候选 mask，不得当作金标
  reviewed/images/     # 已复核样本
  reviewed/masks/      # 已复核 mask
  manifests/           # 患者划分、来源、授权和复核状态
```

### 4.2 半自动标注流程

```text
DENTEX/人工检测框 -> 扩大框 5%-10% -> MedSAM/LiteMedSAM 生成 3 个候选
                        -> 选最合理的一个 -> 必要时加前景/背景点修正
                        -> 记录复核状态 -> 导出 PNG mask/COCO JSON
```

这条路线可以大幅减少描边工作，但 MedSAM 输出是候选标注，不是牙科真值。龋病在全景片上边界弱且尺寸小，必须保留来源、生成模型版本、提示框和复核记录。

适合 8GB 显存的方案不是整张高分辨率全景片，而是：

```text
YOLO/牙位模型找到牙齿 -> 裁剪单牙 ROI -> 256x256 或 384x384
                      -> TransUNet 二分类分割 -> 映射回原图
```

建议起始配置：

- 模型：`R50-ViT-B_16`，使用预训练初始化。
- 输入：单牙 ROI，先 `256x256`；稳定后测试 `384x384`。
- batch：256 时从 2 开始，384 时从 1 开始。
- 精度：AMP/FP16。
- 优化器：AdamW，初始学习率约 `1e-4`；若 batch 减半，学习率也相应减小。
- 损失：Dice + BCE；类别极不均衡时再评估 Focal/Tversky。
- 训练：100-200 epoch，验证集早停，保存最佳 Dice 而不是最后一轮。
- 增强：小角度旋转、轻微缩放和亮度/对比度扰动；禁止垂直翻转，涉及牙位时禁用水平翻转。

必须保存：训练/验证患者划分、配置、随机种子、初始化权重哈希、最佳任务权重、每类 Dice/IoU、失败样例和外部测试结果。

## 5. 第一版取舍

第一版主线使用轻量目标检测，因为 DENTEX 的标注与它匹配，RTX 4060 训练成本低，也能直接输出可解释的异常框。TransUNet 使用公开分割真值建立学术基线；得到少量本地复核数据后再做域适配和外部测试。不应为了页面显示“热力图/掩膜”而把未复核伪 mask 当成临床真值。
