我将根据 `app-ui-design/SKILL.md` 规范，对 App 的前端 UI 进行全面优化。

### 1. 配色方案更新 (Color Palette)

更新 [Theme.kt](file:///d:/IdeaProjects/xl/app/src/main/java/com/xltool/quadrant/ui/theme/Theme.kt) 中的配色，确保符合“深海蓝”专业风格：

* **主色 (Primary)**: `#1E3A5F` (深海蓝)

* **辅助色 (Secondary)**: `#64748B` (灰蓝色)

* **点缀色 (Tertiary)**: `#94A3B8` (浅灰色)

* **背景色**: 浅灰色背景，卡片使用纯白色。

# 2. 字体排版标准化 (Typography)

由于项目目前使用的是默认字体，我将新建 `Type.kt` 并定义一套符合规范的 `Typography`：

* **大标题 (HeadlineLarge)**: 32sp，加粗。

* **小标题 (TitleLarge)**: 24sp，加粗。

* **正文 (BodyLarge)**: 16sp (对应规范中的 21pt)，常规。

* **辅助文字 (BodySmall)**: 14sp，浅灰色。

### 3. 核心组件优化 (Component Refinement)

* **任务卡片 ([TaskCard.kt](file:///d:/IdeaProjects/xl/app/src/main/java/com/xltool/quadrant/ui/home/TaskCard.kt))**:

  * 圆角统一设置为 `8.dp`。

  * 内边距调整为 `16.dp`。

  * 添加轻微阴影 (`elevation = 4.dp`) 营造悬浮感。

* **任务编辑器 ([TaskEditorSheet.kt](file:///d:/IdeaProjects/xl/app/src/main/java/com/xltool/quadrant/ui/home/TaskEditorSheet.kt))**:

  * **输入框**: 优化 `OutlinedTextField` 的边框颜色，聚焦时使用主色高亮。

  * **按钮**: 确保主要操作（保存）使用深海蓝填充，次要操作（取消）使用文字或描边样式。

  * **间距**: 严格遵循 8px 倍数的间距系统。

### 4. 验证与预览

* 运行构建验证代码正确性。

* 检查所有界面是否达到了“简洁、大方、得体”的设计要求。

