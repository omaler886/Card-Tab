const HTML_CONTENT = `
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Card Tab</title>
    <link rel="icon" href="data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><text y=%22.9em%22 font-size=%2280%22>⭐</text></svg>">
    <style>
    /* 全局样式 */
    body {
        font-family: 'Segoe UI', -apple-system, BlinkMacSystemFont, Roboto, Oxygen, Ubuntu, Cantarell, 'Open Sans', 'Helvetica Neue', sans-serif;
        margin: 0;
        padding: 0;
        position: relative;
        overflow-x: hidden;
        background-color: #f8f6f2; /* 米白色背景 */
        background-image: radial-gradient(circle at top, rgba(67, 184, 131, 0.08), transparent 38%), radial-gradient(circle at bottom right, rgba(93, 127, 185, 0.08), transparent 30%);
        background-attachment: fixed;
        color: #222; /* 深灰字体 */
        transition: all 0.3s ease;
        min-height: 100vh;
    }

    .ambient-background {
        position: fixed;
        inset: -18vh -14vw;
        pointer-events: none;
        z-index: 0;
        overflow: hidden;
    }

    .ambient-grid {
        position: absolute;
        inset: 0;
        background-image: linear-gradient(rgba(67, 184, 131, 0.045) 1px, transparent 1px), linear-gradient(90deg, rgba(93, 127, 185, 0.045) 1px, transparent 1px);
        background-size: 110px 110px;
        opacity: 0.35;
        animation: ambientGridPan 34s linear infinite alternate;
    }

    .ambient-orb {
        position: absolute;
        width: 40vw;
        height: 40vw;
        min-width: 240px;
        min-height: 240px;
        max-width: 680px;
        max-height: 680px;
        border-radius: 50%;
        filter: blur(80px);
        opacity: 0.46;
        will-change: transform;
        transition: transform 16s ease-in-out, opacity 9s ease, background 10s ease;
    }

    .ambient-orb.orb-a { background: rgba(67, 184, 131, 0.36); }
    .ambient-orb.orb-b { background: rgba(93, 127, 185, 0.28); }
    .ambient-orb.orb-c { background: rgba(255, 194, 102, 0.22); }

    body.dark-theme .ambient-grid {
        opacity: 0.16;
        background-image: linear-gradient(rgba(93, 127, 185, 0.08) 1px, transparent 1px), linear-gradient(90deg, rgba(67, 184, 131, 0.06) 1px, transparent 1px);
    }

    body.dark-theme .ambient-orb {
        opacity: 0.28;
    }

    @keyframes ambientGridPan {
        from { transform: translate3d(-3%, -2%, 0); }
        to { transform: translate3d(3%, 2%, 0); }
    }

    /* 暗色模式样式 */
    body.dark-theme {
        background-color: #121418; /* 更深的背景色 */
        background-image: radial-gradient(circle at top, rgba(93, 127, 185, 0.16), transparent 40%), radial-gradient(circle at bottom right, rgba(67, 184, 131, 0.1), transparent 32%);
        color: #e3e3e3;
    }

    /* 固定元素样式 */
    .fixed-elements {
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        background: rgba(248, 246, 242, 0.72);
        border-bottom: 1px solid rgba(67, 184, 131, 0.12);
        z-index: 1000;
        padding: 10px;
        transition: all 0.3s ease;
        height: 150px;
        box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
        backdrop-filter: blur(18px) saturate(140%);
        -webkit-backdrop-filter: blur(18px) saturate(140%);
    }

    body.dark-theme .fixed-elements {
        background: rgba(18, 20, 24, 0.76);
        border-bottom-color: rgba(93, 127, 185, 0.18);
        box-shadow: 0 10px 28px rgba(0, 0, 0, 0.28);
    }

    /* 分类快捷按钮容器样式移至搜索栏内 */

    .category-button {
        padding: 5px 10px;
        border-radius: 15px;
        background-color: #f9fafb;
        color: #43b883;
        border: none;
        cursor: pointer;
        font-size: 12px;
        font-weight: 500;
        transition: all 0.2s ease;
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
        flex: 0 0 auto;
        white-space: nowrap;
        margin: 0 2px;
        position: relative;
        overflow: hidden;
    }

    body.dark-theme .category-button {
        background-color: #2a2e38;
        color: #5d7fb9;
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
    }

    .category-button:hover {
        background-color: #43b883;
        color: white;
        transform: translateY(-1px);
        box-shadow: 0 3px 5px rgba(0, 0, 0, 0.12);
    }

    /* 分类按钮选中效果 */
    .category-button.active {
        background-color: #43b883;
        color: white;
        box-shadow: 0 2px 5px rgba(0, 0, 0, 0.12);
        transform: translateY(-1px);
        font-weight: 600;
        border-bottom: 2px solid #35a674;
    }

    body.dark-theme .category-button:hover,
    body.dark-theme .category-button.active {
        background-color: #5d7fb9;
        color: white;
    }

    /* 分类按钮悬停样式 */

    .fixed-elements h3 {
        position: absolute;
        top: 10px;
        left: 20px;
        margin: 0;
        font-size: 22px;
        font-weight: 600;
        color: #222;
        transition: all 0.3s ease;
        z-index: 10;
    }

    body.dark-theme .fixed-elements h3 {
        color: #e3e3e3;
    }

    /* 一言模块样式 */
    #hitokoto {
        margin: 5px 0 15px;
        font-size: 14px;
        color: #888;
        font-style: italic;
        max-width: 600px;
        margin-left: auto;
        margin-right: auto;
        transition: all 0.3s ease;
    }

    #hitokoto a {
        color: #43b883;
        text-decoration: none;
        transition: all 0.3s ease;
    }

    #hitokoto a:hover {
        color: #35a674;
    }

    body.dark-theme #hitokoto {
        color: #a0a0a0;
    }

    body.dark-theme #hitokoto a {
        color: #5d7fb9;
    }

    /* 中心内容样式 */
    .center-content {
        position: absolute;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
        width: 100%;
        max-width: none; /* 不限制最大宽度，使分类按钮有更多空间 */
        text-align: center;
        padding: 0 10px; /* 添加左右内边距 */
    }

    /* 右上角控制区域样式 */
    .top-right-controls {
        position: fixed;
        top: 10px;
        right: 10px;
        display: flex;
        align-items: center;
        gap: 10px;
        z-index: 1001;
    }

    /* 设置按钮样式 */
    .admin-btn {
        background-color: #43b883;
        color: white;
        border: none;
        border-radius: 4px;
        padding: 8px 16px;
        font-size: 13px;
        cursor: pointer;
        transition: all 0.3s ease;
        font-weight: 500;
    }

    .admin-btn:hover {
        background-color: #35a674;
        transform: translateY(-1px);
    }

    body.dark-theme .admin-btn {
        background-color: #5d7fb9;
    }

    body.dark-theme .admin-btn:hover {
        background-color: #4a6fa5;
    }

    /* 登录按钮样式 */
    .login-btn {
        background-color: #43b883;
        color: white;
        border: none;
        border-radius: 4px;
        padding: 8px 16px;
        font-size: 13px;
        cursor: pointer;
        transition: all 0.3s ease;
        font-weight: 500;
    }

    .login-btn:hover {
        background-color: #35a674;
        transform: translateY(-1px);
    }

    body.dark-theme .login-btn {
        background-color: #5d7fb9;
    }

    body.dark-theme .login-btn:hover {
        background-color: #4a6fa5;
    }

    /* GitHub图标按钮样式 */
    .github-btn {
        background: none;
        border: none;
        cursor: pointer;
        transition: all 0.3s ease;
        display: flex;
        align-items: center;
        justify-content: center;
        width: 36px;
        height: 36px;
        border-radius: 4px;
        padding: 0;
    }

    .github-btn:hover {
        transform: translateY(-2px);
    }

    .github-btn svg {
        width: 24px;
        height: 24px;
        fill: #43b883;
        transition: fill 0.3s ease;
    }

    body.dark-theme .github-btn svg {
        fill: #5d7fb9;
    }

    /* 书签搜索图标按钮样式 */
    .bookmark-search-toggle {
        background-color: #43b883;
        color: white;
        border: none;
        border-radius: 4px;
        padding: 0;
        cursor: pointer;
        transition: all 0.3s ease;
        display: flex;
        align-items: center;
        justify-content: center;
        width: 36px;
        height: 36px;
        position: relative;
    }

    .bookmark-search-toggle:hover {
        background-color: #35a674;
        transform: translateY(-2px);
    }

    .bookmark-search-toggle svg {
        width: 20px;
        height: 20px;
        stroke: white;
    }

    body.dark-theme .bookmark-search-toggle {
        background-color: #5d7fb9;
    }

    body.dark-theme .bookmark-search-toggle:hover {
        background-color: #4a6fa5;
    }

    /* 下拉书签搜索框样式 */
    .bookmark-search-dropdown {
        position: absolute;
        top: 100%;
        right: 0;
        width: 140px;
        background-color: white;
        border: 1px solid #e0e0e0;
        border-radius: 4px;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
        padding: 8px;
        margin-top: 4px;
        display: none;
        z-index: 1002;
    }

    .bookmark-search-dropdown.show {
        display: block;
    }

    .bookmark-search-dropdown input {
        width: 100%;
        border: 1px solid #e0e0e0;
        border-radius: 4px;
        padding: 8px 12px;
        font-size: 13px;
        transition: all 0.3s ease;
        box-sizing: border-box;
    }

    .bookmark-search-dropdown input:focus {
        border-color: #43b883;
        box-shadow: 0 0 0 2px rgba(67, 184, 131, 0.2);
        outline: none;
    }

    .bookmark-search-dropdown input::placeholder {
        color: #999;
    }

    body.dark-theme .bookmark-search-dropdown {
        background-color: #323642;
        border-color: #444;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
    }

    body.dark-theme .bookmark-search-dropdown input {
        background-color: #252830;
        color: #e3e3e3;
        border-color: #444;
    }

    body.dark-theme .bookmark-search-dropdown input::placeholder {
        color: #888;
    }

    /* 登录弹窗样式 */
    .login-modal {
        display: none;
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background-color: rgba(0, 0, 0, 0.6);
        justify-content: center;
        align-items: center;
        z-index: 2000;
        backdrop-filter: blur(3px);
    }

    .login-modal-content {
        background-color: white;
        padding: 25px;
        border-radius: 10px;
        width: 300px;
        box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
        animation: modalFadeIn 0.3s ease;
    }

    @keyframes modalFadeIn {
        from { opacity: 0; transform: translateY(-20px); }
        to { opacity: 1; transform: translateY(0); }
    }

    .login-modal h3 {
        margin: 0 0 20px 0;
        color: #333;
        text-align: center;
        font-size: 18px;
    }

    .login-modal input {
        width: 100%;
        margin-bottom: 15px;
        padding: 10px;
        border: 1px solid #e0e0e0;
        border-radius: 5px;
        font-size: 14px;
        transition: all 0.3s ease;
        box-sizing: border-box;
    }

    .login-modal input:focus {
        border-color: #43b883;
        box-shadow: 0 0 0 2px rgba(67, 184, 131, 0.2);
        outline: none;
    }

    .login-modal-buttons {
        display: flex;
        gap: 10px;
        justify-content: flex-end;
    }

    .login-modal button {
        background-color: #43b883;
        color: white;
        border: none;
        padding: 10px 15px;
        border-radius: 5px;
        cursor: pointer;
        transition: all 0.3s ease;
        font-size: 13px;
    }

    .login-modal button:hover {
        background-color: #35a674;
    }

    .login-modal button.cancel {
        background-color: #f0f0f0;
        color: #333;
    }

    .login-modal button.cancel:hover {
        background-color: #e0e0e0;
    }

    body.dark-theme .login-modal-content {
        background-color: #252830;
        color: #e3e3e3;
    }

    body.dark-theme .login-modal h3 {
        color: #e3e3e3;
    }

    body.dark-theme .login-modal input {
        background-color: #323642;
        color: #e3e3e3;
        border-color: #444;
    }

    /* 悬浮提示样式 */
    @media (hover: hover) and (pointer: fine) {
        .has-tooltip {
            position: relative;
        }

        .has-tooltip::after {
            content: attr(data-tooltip);
            position: absolute;
            background: rgba(0, 0, 0, 0.75);
            color: white;
            padding: 6px 10px;
            border-radius: 4px;
            font-size: 12px;
            pointer-events: none;
            opacity: 0;
            transition: opacity 0.3s;
            white-space: nowrap;
            z-index: 1000;
        }

        .has-tooltip::before {
            content: "";
            position: absolute;
            border: 6px solid transparent;
            opacity: 0;
            transition: opacity 0.3s;
            z-index: 1000;
        }

        .has-tooltip:hover::after,
        .has-tooltip:hover::before {
            opacity: 1;
        }

        /* 下方提示框和箭头 */
        .tooltip-bottom::after {
            top: 100%;
            left: 50%;
            margin-top: 12px;
            transform: translateX(-50%);
        }
        .tooltip-bottom::before {
            top: 100%;
            left: 50%;
            transform: translateX(-50%);
            border-bottom-color: rgba(0, 0, 0, 0.75);
        }

        /* 绿底样式 */
        .tooltip-green::after {
            background: #43b883;
            color: white;
        }
        .tooltip-green::before {
            border-bottom-color: #43b883;
        }

        /* 暗色主题 */
        body.dark-theme .has-tooltip::after {
            background: rgba(151, 151, 151, 0.9);
            color: #eee;
        }
        body.dark-theme .has-tooltip::before {
            border-bottom-color: rgba(151, 151, 151, 0.9);
        }
        body.dark-theme .tooltip-green::after {
            background: #5d7fb9;
            color: white;
        }
        body.dark-theme .tooltip-green::before {
            border-bottom-color: #5d7fb9;
        }
    }

    /* 搜索结果样式 - 简化版 */
    .search-results-section {
        margin-bottom: 30px;
    }

    .search-results-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 20px;
        padding: 15px;
        background-color: #f8f9fa;
        border-radius: 8px;
        border-left: 4px solid #43b883;
    }

    body.dark-theme .search-results-header {
        background-color: #2d3748;
        border-left-color: #5d7fb9;
    }

    .search-results-title {
        font-size: 18px;
        font-weight: bold;
        color: #333;
    }

    body.dark-theme .search-results-title {
        color: #e2e8f0;
    }

    .back-to-main {
        background-color: #43b883;
        color: white;
        border: none;
        border-radius: 4px;
        padding: 8px 16px;
        cursor: pointer;
        font-size: 14px;
        transition: all 0.3s ease;
    }

    .back-to-main:hover {
        background-color: #35a674;
    }

    body.dark-theme .back-to-main {
        background-color: #5d7fb9;
    }

    body.dark-theme .back-to-main:hover {
        background-color: #4a6fa5;
    }

    .no-search-results {
        text-align: center;
        padding: 30px;
        color: #888;
        font-size: 16px;
    }

    body.dark-theme .no-search-results {
        color: #a0a0a0;
    }

    /* 管理控制按钮样式 - 严格按照佬友修改版设计 */
    .add-remove-controls {
        display: none;
        flex-direction: column;
        position: fixed;
        right: 20px;
        top: 50%;
        transform: translateY(-50%);
        align-items: center;
        gap: 15px;
        z-index: 900;
    }

    .round-btn {
        background-color: #43b883;
        color: white;
        border: none;
        border-radius: 50%;
        width: 40px;
        height: 40px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 22px;
        cursor: pointer;
        box-shadow: 0 3px 10px rgba(0, 0, 0, 0.15);
        transition: all 0.3s ease;
        position: relative;
    }

    .round-btn svg {
        pointer-events: none;
        display: block;
        margin: auto;
    }

    .round-btn:hover {
        transform: translateY(-3px);
        box-shadow: 0 5px 15px rgba(0, 0, 0, 0.2);
    }

    body.dark-theme .round-btn {
        background-color: #5d7fb9;
    }

    .add-btn { order: 1; }
    .remove-btn { order: 2; }
    .category-btn { order: 3; }
    .remove-category-btn { order: 4; }

    /* 主要内容区域样式 */
    .content {
        margin-top: 170px;
        padding: 10px;
        max-width: 1600px;
        margin-left: auto;
        margin-right: auto;
        position: relative;
        z-index: 1;
        transition: opacity 0.3s ease;
    }

    .loading .content {
        opacity: 0.6;
    }

    .boot-status {
        margin: 16px auto 24px;
        max-width: 880px;
        padding: 16px 20px;
        border-radius: 16px;
        background: rgba(255, 255, 255, 0.66);
        border: 1px solid rgba(67, 184, 131, 0.12);
        color: #5e6975;
        font-size: 14px;
        line-height: 1.7;
        text-align: center;
        box-shadow: 0 14px 36px rgba(15, 23, 42, 0.08);
        backdrop-filter: blur(18px);
        -webkit-backdrop-filter: blur(18px);
    }

    .boot-status.error {
        color: #9c3d3d;
        border-color: rgba(211, 84, 84, 0.2);
        background: rgba(255, 245, 245, 0.78);
    }

    body.dark-theme .boot-status {
        background: rgba(30, 33, 40, 0.74);
        border-color: rgba(93, 127, 185, 0.16);
        color: #a7b3bf;
        box-shadow: 0 16px 40px rgba(0, 0, 0, 0.22);
    }

    body.dark-theme .boot-status.error {
        color: #f1b7b7;
        border-color: rgba(214, 125, 125, 0.24);
        background: rgba(60, 32, 35, 0.78);
    }

    /* 搜索栏样式 */
    .search-container {
        margin-top: 10px;
        display: flex;
        flex-direction: column;
        align-items: center;
        width: 100%;
    }

    .search-bar {
        display: flex;
        justify-content: center;
        margin-bottom: 10px;
        width: 100%;
        max-width: 600px;
        margin-left: auto;
        margin-right: auto;
        border-radius: 8px;
        overflow: hidden;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
        border: 1px solid #e0e0e0;
        transition: all 0.3s ease;
    }

    .search-bar:focus-within {
        box-shadow: 0 3px 12px rgba(0, 0, 0, 0.1);
        border-color: #43b883;
    }

    .search-bar select {
        border: none;
        background-color: #f4f7fa;
        padding: 10px 15px;
        font-size: 14px;
        color: #43b883;
        width: 120px;
        outline: none;
        -webkit-appearance: none;
        -moz-appearance: none;
        appearance: none;
        background-image: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="12" height="6" viewBox="0 0 12 6"><path fill="%2343b883" d="M0 0l6 6 6-6z"/></svg>');
        background-repeat: no-repeat;
        background-position: right 10px center;
        cursor: pointer;
        transition: all 0.3s ease;
        border-radius: 0;
    }

    /* 下拉菜单样式 */
    select option {
        background-color: #fff;
        color: #333;
        padding: 10px;
        font-size: 14px;
        white-space: nowrap;
        overflow: visible;
    }

    /* 暗色主题搜索栏样式 */
    body.dark-theme .search-bar {
        border-color: #323642;
        background-color: #1e2128;
    }

    body.dark-theme .search-bar select {
        background-color: #252830;
        color: #5d7fb9;
        background-image: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="12" height="6" viewBox="0 0 12 6"><path fill="%235d7fb9" d="M0 0l6 6 6-6z"/></svg>');
    }

    body.dark-theme .search-bar input {
        background-color: #252830;
        color: #e3e3e3;
    }

    body.dark-theme .search-bar button {
        background-color: #5d7fb9;
    }

    body.dark-theme select option {
        background-color: #252830;
        color: #e3e3e3;
        white-space: nowrap;
        overflow: visible;
    }

    .search-bar input {
        flex: 1;
        border: none;
        padding: 10px 15px;
        font-size: 14px;
        background-color: #fff;
        outline: none;
    }

    .search-bar button {
        border: none;
        background-color: #43b883;
        color: white;
        padding: 0 20px;
        cursor: pointer;
        transition: background-color 0.3s;
    }

    .search-bar button:hover {
        background-color: #35a674;
    }

    /* 分类按钮容器样式 - 移至固定元素区域内 */
    .category-buttons-container {
        display: flex;
        flex-wrap: wrap; /* 允许按钮换行显示 */
        justify-content: center; /* 居中排列按钮 */
        gap: 6px;
        padding: 8px 12px;
        width: 100%;
        max-width: 1200px; /* 增加容器宽度，确保能显示更多按钮 */
        margin-left: auto;
        margin-right: auto;
        margin-top: 5px; /* 减少与搜索栏的距离 */
        background-color: transparent; /* 背景透明 */
        border-radius: 8px;
        box-shadow: none; /* 移除阴影 */
        transition: all 0.3s ease;
        position: relative; /* 确保在固定元素内正确定位 */
    }

    body.dark-theme .category-buttons-container {
        background-color: transparent; /* 暗色模式下的背景透明 */
        box-shadow: none;
    }

    /* 滚动条美化 */
    ::-webkit-scrollbar {
        width: 8px;
        height: 8px;
    }

    ::-webkit-scrollbar-track {
        background: #f1f1f1;
        border-radius: 4px;
    }

    ::-webkit-scrollbar-thumb {
        background: #c1c1c1;
        border-radius: 4px;
    }

    ::-webkit-scrollbar-thumb:hover {
        background: #a8a8a8;
    }

    body.dark-theme::-webkit-scrollbar-track {
        background: #252830;
    }

    body.dark-theme::-webkit-scrollbar-thumb {
        background: #444;
    }

    body.dark-theme::-webkit-scrollbar-thumb:hover {
        background: #555;
    }

    /* 分类按钮容器滚动条 */
    .category-buttons-container::-webkit-scrollbar {
        height: 4px;
    }

    /* 浮动按钮组样式 */
    .floating-button-group {
        position: fixed;
        bottom: 50px;
        right: 20px;
        display: flex;
        flex-direction: column;
        gap: 15px;
        z-index: 1000;
    }

    .floating-button-group button {
        width: 40px;
        height: 40px;
        border-radius: 50%;
        font-size: 20px;
        display: flex;
        align-items: center;
        justify-content: center;
        background-color: #43b883;
        color: white;
        border: none;
        cursor: pointer;
        box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
        transition: all 0.2s ease;
    }

    .floating-button-group button:hover {
        transform: translateY(-2px);
        background-color: #369f6b;
    }

    #back-to-top-btn {
        display: none;
    }

    body.dark-theme .floating-button-group button {
        background-color: #5d7fb9;
    }

    body.dark-theme .floating-button-group button:hover {
        background-color: #4a6fa5;
    }

    /* 主题切换按钮样式 */
    #theme-toggle {
        font-size: 15px;
        line-height: 40px;
        font-weight: 700;
        letter-spacing: 1px;
    }

    #theme-toggle[data-theme-mode="auto"] {
        background: linear-gradient(135deg, #43b883, #5d7fb9);
    }

    #theme-toggle[data-theme-mode="light"] {
        background: linear-gradient(135deg, #43b883, #35a674);
    }

    #theme-toggle[data-theme-mode="dark"] {
        background: linear-gradient(135deg, #4f6288, #232833);
    }

    /* 对话框样式 */
    #dialog-overlay {
        display: none;
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background-color: rgba(0, 0, 0, 0.6);
        justify-content: center;
        align-items: center;
        z-index: 2000;
        backdrop-filter: blur(3px);
        transition: all 0.3s ease;
    }

    #dialog-box {
        background-color: white;
        padding: 25px;
        border-radius: 10px;
        width: 350px;
        box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
        animation: dialogFadeIn 0.3s ease;
    }

    @keyframes dialogFadeIn {
        from { opacity: 0; transform: translateY(-20px); }
        to { opacity: 1; transform: translateY(0); }
    }

    #dialog-box input, #dialog-box select {
        width: 100%;
        margin-bottom: 15px;
        padding: 10px;
        border: 1px solid #e0e0e0;
        border-radius: 5px;
        font-size: 14px;
        transition: all 0.3s ease;
    }

    #dialog-box input:focus, #dialog-box select:focus {
        border-color: #43b883;
        box-shadow: 0 0 0 2px rgba(67, 184, 131, 0.2);
        outline: none;
    }

    #dialog-box label {
        display: block;
        margin-bottom: 5px;
        font-weight: 500;
        color: #222;
    }

    #dialog-box button {
        background-color: #43b883;
        color: white;
        border: none;
        padding: 10px 15px;
        border-radius: 5px;
        cursor: pointer;
        transition: all 0.3s ease;
        margin-right: 10px;
    }

    #dialog-box button:hover {
        background-color: #35a674;
    }

    #dialog-box button.cancel {
        background-color: #f0f0f0;
        color: #333;
    }

    #dialog-box button.cancel:hover {
        background-color: #e0e0e0;
    }

    body.dark-theme #dialog-box {
        background-color: #252830;
        color: #e3e3e3;
    }

    body.dark-theme #dialog-box input,
    body.dark-theme #dialog-box select {
        background-color: #323642;
        color: #e3e3e3;
        border-color: #444;
    }

    body.dark-theme #dialog-box label {
        color: #a0b7d4;
    }

    /* 分类和卡片样式 */
    .section {
        margin-bottom: 25px;
        padding: 0 15px;
    }

    .section-title-container {
        display: flex;
        align-items: center;
        margin-bottom: 18px;
        border-bottom: 1px solid #e0e0e0;
        padding-bottom: 10px;
        transition: border-color 0.3s ease;
        width: 100%;
        max-width: 1520px;
        margin-left: auto;
        margin-right: auto;
    }

    body.dark-theme .section-title-container {
        border-bottom-color: #2a2e38;
    }

    .section-title {
        font-size: 22px;
        font-weight: 600;
        color: #222;
        position: relative;
        padding-left: 15px;
        transition: color 0.3s ease;
        min-width: 120px;
    }

    body.dark-theme .section-title {
        color: #e3e3e3;
    }

    .section-title:before {
        content: '';
        position: absolute;
        left: 0;
        top: 50%;
        transform: translateY(-50%);
        width: 5px;
        height: 22px;
        background-color: #43b883;
        border-radius: 2px;
    }

    .delete-category-btn {
        background-color: #ff9800;
        color: white;
        border: none;
        padding: 6px 12px;
        border-radius: 5px;
        cursor: pointer;
        margin-left: 15px;
        font-size: 13px;
        transition: all 0.3s ease;
        box-shadow: 0 2px 5px rgba(0, 0, 0, 0.08);
    }

    .delete-category-btn:hover {
        background-color: #f57c00;
        box-shadow: 0 3px 8px rgba(0, 0, 0, 0.15);
    }

    body.dark-theme .delete-category-btn {
        background-color: #ff9800;
        color: #252830;
    }

    .card-container {
        display: grid;
        grid-template-columns: repeat(auto-fit, 150px);
        column-gap: 35px;
        row-gap: 15px;
        justify-content: start;
        padding: 15px;
        padding-left: 45px;
        margin: 0 auto;
        max-width: 1600px;
    }

    .card {
        background-color: white;
        border-radius: 8px;
        padding: 12px;
        width: 150px;
        box-shadow: 0 3px 10px rgba(0, 0, 0, 0.06);
        cursor: pointer;
        transition: all 0.3s ease;
        position: relative;
        user-select: none;
        border-left: 3px solid #43b883;
        animation: fadeIn 0.3s ease forwards;
        animation-delay: calc(var(--card-index) * 0.05s);
        opacity: 0;
        margin: 2px;
    }

    body.dark-theme .card {
        background-color: #1e2128; /* 卡片背景 */
        border-left-color: #5d7fb9;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
    }

    @keyframes fadeIn {
        from { opacity: 0; transform: translateY(10px); }
        to { opacity: 1; transform: translateY(0); }
    }

    .card:hover {
        transform: translateY(-5px);
        box-shadow: 0 8px 15px rgba(0, 0, 0, 0.08);
    }

    .card-top {
        display: flex;
        align-items: center;
        margin-bottom: 5px;
    }

    .card-icon {
        width: 16px;
        height: 16px;
        margin-right: 5px;
    }

    .card-title {
        font-size: 15px;
        font-weight: 600;
        color: #222;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        transition: color 0.3s ease;
    }

    .card-url {
        font-size: 12px;
        color: #888;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        transition: color 0.3s ease;
    }

    body.dark-theme .card-title {
        color: #e3e3e3;
    }

    body.dark-theme .card-url {
        color: #a0a0a0;
    }

    .private-tag {
        background-color: #ff9800;
        color: white;
        font-size: 10px;
        padding: 2px 5px;
        border-radius: 3px;
        position: absolute;
        top: 18px;
        right: 5px;
        z-index: 5;
    }





    /* 版权信息样式 */
    #copyright {
        position: fixed;
        bottom: 0;
        left: 0;
        width: 100%;
        height: 40px;
        background-color: rgba(255, 255, 255, 0.9);
        display: flex;
        justify-content: center;
        align-items: center;
        font-size: 14px;
        z-index: 1000;
        box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.03);
        backdrop-filter: blur(5px);
        transition: all 0.3s ease;
    }

    #copyright p {
        margin: 0;
        font-weight: 500;
        color: #666;
    }

    #copyright a {
        color: #43b883;
        text-decoration: none;
        transition: all 0.3s ease;
        position: relative;
    }

    #copyright a:after {
        content: '';
        position: absolute;
        width: 100%;
        height: 1px;
        bottom: 0;
        left: 0;
        background-color: #43b883;
        transform: scaleX(0);
        transition: transform 0.3s ease;
    }

    #copyright a:hover:after {
        transform: scaleX(1);
    }

    body.dark-theme #copyright {
        background-color: rgba(37, 40, 48, 0.9);
        color: #e3e3e3;
    }

    body.dark-theme #copyright a {
        color: #5d7fb9;
    }

    body.dark-theme #copyright a:after {
        background-color: #5d7fb9;
    }

    /* ========== 天气组件样式 ========== */
    .weather-mini {
        display: inline-flex;
        align-items: center;
        gap: 6px;
        padding: 4px 10px;
        background: rgba(255, 255, 255, 0.5);
        border: 1px solid rgba(67, 184, 131, 0.18);
        border-radius: 16px;
        cursor: pointer;
        transition: all 0.2s ease;
        font-size: 13px;
        margin-left: 12px;
        vertical-align: middle;
        backdrop-filter: blur(14px) saturate(150%);
        -webkit-backdrop-filter: blur(14px) saturate(150%);
        box-shadow: 0 6px 18px rgba(67, 184, 131, 0.08);
    }
    .weather-mini:hover {
        background: rgba(255, 255, 255, 0.68);
        transform: translateY(-1px);
    }
    .weather-mini .weather-icon { font-size: 16px; }
    .weather-mini .weather-temp { font-weight: 600; color: #333; }
    .weather-mini .weather-city { color: #666; font-size: 12px; }
    .weather-mini .weather-loading { color: #999; font-size: 12px; }

    body.dark-theme .weather-mini {
        background: rgba(30, 33, 40, 0.54);
        border-color: rgba(93, 127, 185, 0.22);
        box-shadow: 0 10px 24px rgba(0, 0, 0, 0.2);
    }
    body.dark-theme .weather-mini:hover {
        background: rgba(40, 44, 54, 0.72);
    }
    body.dark-theme .weather-mini .weather-temp { color: #e3e3e3; }
    body.dark-theme .weather-mini .weather-city { color: #aaa; }

    /* 天气弹窗 */
    .weather-modal {
        display: none;
        position: fixed;
        top: 0; left: 0; right: 0; bottom: 0;
        background: rgba(7, 10, 15, 0.36);
        z-index: 2000;
        justify-content: center;
        align-items: center;
        backdrop-filter: blur(12px);
        -webkit-backdrop-filter: blur(12px);
    }
    .weather-modal.show { display: flex; }
    .weather-modal-content {
        background: rgba(255, 255, 255, 0.78);
        border: 1px solid rgba(255, 255, 255, 0.5);
        border-radius: 16px;
        padding: 20px;
        width: 90%;
        max-width: 360px;
        box-shadow: 0 10px 40px rgba(0,0,0,0.2);
        animation: weatherModalIn 0.25s ease;
        backdrop-filter: blur(24px) saturate(145%);
        -webkit-backdrop-filter: blur(24px) saturate(145%);
    }
    @keyframes weatherModalIn {
        from { opacity: 0; transform: scale(0.9) translateY(-20px); }
        to { opacity: 1; transform: scale(1) translateY(0); }
    }
    .weather-modal-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 16px;
        padding-bottom: 12px;
        border-bottom: 1px solid #eee;
    }
    .weather-modal-title { font-size: 16px; font-weight: 600; color: #333; }
    .weather-modal-close {
        background: none; border: none;
        font-size: 20px; cursor: pointer;
        color: #999; padding: 0; line-height: 1;
    }
    .weather-modal-close:hover { color: #333; }

    /* 城市搜索 */
    .weather-search {
        position: relative;
        margin-bottom: 16px;
    }
    .weather-search input {
        width: 100%;
        padding: 10px 12px;
        border: 1px solid #ddd;
        border-radius: 8px;
        font-size: 14px;
        outline: none;
        box-sizing: border-box;
    }
    .weather-search input:focus { border-color: #43b883; }
    .weather-search-results {
        position: absolute;
        top: 100%;
        left: 0; right: 0;
        background: rgba(255, 255, 255, 0.92);
        border: 1px solid #ddd;
        border-radius: 8px;
        margin-top: 4px;
        max-height: 200px;
        overflow-y: auto;
        display: none;
        z-index: 10;
        backdrop-filter: blur(16px);
        -webkit-backdrop-filter: blur(16px);
    }
    .weather-search-results.show { display: block; }
    .weather-search-item {
        padding: 10px 12px;
        cursor: pointer;
        border-bottom: 1px solid #eee;
    }
    .weather-search-item:last-child { border-bottom: none; }
    .weather-search-item:hover { background: #f5f5f5; }
    .weather-search-item-name { font-weight: 500; font-size: 14px; }
    .weather-search-item-path { font-size: 12px; color: #999; margin-top: 2px; }

    /* 定位模式切换 */
    .weather-mode-switch {
        display: flex;
        gap: 8px;
        margin-bottom: 16px;
    }
    .weather-mode-btn {
        flex: 1;
        padding: 8px 12px;
        border: 1px solid #ddd;
        border-radius: 8px;
        background: #fff;
        font-size: 13px;
        cursor: pointer;
        transition: all 0.2s;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 4px;
    }
    .weather-mode-btn:hover { border-color: #43b883; }
    .weather-mode-btn.active {
        background: #43b883;
        border-color: #43b883;
        color: #fff;
    }

    .weather-source-note {
        margin-bottom: 14px;
        padding: 10px 12px;
        border-radius: 12px;
        background: rgba(255, 255, 255, 0.58);
        border: 1px solid rgba(67, 184, 131, 0.12);
        font-size: 12px;
        color: #61707d;
        line-height: 1.55;
        backdrop-filter: blur(16px);
        -webkit-backdrop-filter: blur(16px);
    }

    .weather-source-note strong {
        color: #2d3a45;
    }
    body.dark-theme .weather-mode-btn {
        background: #3a3a3a;
        border-color: #555;
        color: #e3e3e3;
    }
    body.dark-theme .weather-mode-btn:hover { border-color: #43b883; }
    body.dark-theme .weather-mode-btn.active {
        background: #43b883;
        border-color: #43b883;
        color: #fff;
    }
    body.dark-theme .weather-source-note {
        background: rgba(42, 46, 56, 0.78);
        border-color: rgba(93, 127, 185, 0.16);
        color: #9ba6b3;
    }
    body.dark-theme .weather-source-note strong { color: #e3e3e3; }

    /* 当前天气 */
    .weather-current {
        text-align: center;
        padding: 16px 0;
        border-bottom: 1px solid #eee;
        margin-bottom: 16px;
    }
    .weather-current-icon { font-size: 48px; margin-bottom: 8px; }
    .weather-current-temp { font-size: 36px; font-weight: 300; color: #333; }
    .weather-current-desc { font-size: 16px; color: #666; margin: 4px 0; }
    .weather-current-detail { font-size: 13px; color: #999; }

    /* 天气预报 */
    .weather-forecast {
        display: flex;
        justify-content: space-between;
        gap: 8px;
    }
    .weather-forecast-item {
        flex: 1;
        text-align: center;
        padding: 12px 8px;
        background: rgba(248, 249, 250, 0.75);
        border: 1px solid rgba(67, 184, 131, 0.08);
        border-radius: 10px;
    }
    .weather-forecast-day { font-size: 13px; font-weight: 500; color: #333; margin-bottom: 6px; }
    .weather-forecast-icon { font-size: 24px; margin: 6px 0; }
    .weather-forecast-temp { font-size: 12px; color: #666; }
    .weather-forecast-temp .high { color: #e74c3c; }
    .weather-forecast-temp .low { color: #3498db; }

    /* 天气未配置状态 */
    .weather-not-configured {
        text-align: center;
        padding: 40px 20px;
        color: #999;
        font-size: 14px;
    }
    .weather-search input:disabled {
        background: #f5f5f5;
        cursor: not-allowed;
        color: #999;
    }
    body.dark-theme .weather-not-configured { color: #666; }
    body.dark-theme .weather-search input:disabled {
        background: #2a2e38;
        color: #666;
    }

    /* 天气弹窗暗色主题 */
    body.dark-theme .weather-modal-content {
        background: rgba(30, 33, 40, 0.78);
        border-color: rgba(93, 127, 185, 0.18);
    }
    body.dark-theme .weather-modal-header { border-bottom-color: #333; }
    body.dark-theme .weather-modal-title { color: #e3e3e3; }
    body.dark-theme .weather-modal-close { color: #888; }
    body.dark-theme .weather-modal-close:hover { color: #e3e3e3; }
    body.dark-theme .weather-search input { background: #2a2e38; border-color: #444; color: #e3e3e3; }
    body.dark-theme .weather-search input:focus { border-color: #5d7fb9; }
    body.dark-theme .weather-search-results { background: rgba(42, 46, 56, 0.92); border-color: #444; }
    body.dark-theme .weather-search-item:hover { background: #333; }
    body.dark-theme .weather-search-item { border-bottom-color: #444; }
    body.dark-theme .weather-current { border-bottom-color: #333; }
    body.dark-theme .weather-current-temp { color: #e3e3e3; }
    body.dark-theme .weather-current-desc { color: #aaa; }
    body.dark-theme .weather-forecast-item { background: rgba(42, 46, 56, 0.82); border-color: rgba(93, 127, 185, 0.16); }
    body.dark-theme .weather-forecast-day { color: #e3e3e3; }
    body.dark-theme .weather-forecast-temp { color: #aaa; }

    /* 响应式设计 */
    @media (max-width: 480px) {
        .fixed-elements {
            position: fixed; /* 恢复固定定位，确保分类按钮位置正确 */
            padding: 8px 12px 5px 12px; /* 紧凑的内边距 */
            height: auto;
            min-height: 140px; /* 增加最小高度，确保有足够空间 */
            box-shadow: none; /* 移除阴影 */
        }

        body.dark-theme .fixed-elements {
            box-shadow: none; /* 移除阴影 */
        }

        /* 移动端一言样式调整 - 紧凑显示 */
        #hitokoto {
            margin: 3px 0 6px 0; /* 紧凑的上下边距 */
            font-size: 12px; /* 减小字体 */
            line-height: 1.3; /* 紧凑行高 */
            padding: 0 8px; /* 左右内边距 */
        }

        .category-buttons-container {
            width: 100%;
            max-width: none;
            padding: 6px;
            overflow-x: auto; /* 允许水平滚动 */
            flex-wrap: nowrap; /* 不允许按钮换行 */
            justify-content: flex-start; /* 左对齐排列按钮 */
            margin: 8px auto 5px; /* 紧凑的分类按钮边距 */
            scrollbar-width: none; /* Firefox */
            -ms-overflow-style: none; /* IE and Edge */
            background-color: transparent; /* 移动端也透明 */
            border-radius: 8px;
            gap: 4px; /* 减小按钮间距 */
        }

        body.dark-theme .category-buttons-container {
            background-color: transparent;
        }

        .category-button {
            padding: 4px 8px;
            font-size: 11px;
            margin: 0 1px;
        }

        .content {
            margin-top: 150px; /* 增加顶部边距，适配更高的固定元素 */
            margin-bottom: 100px; /* 为底部的分类按钮和版权信息留出空间 */
            padding: 15px; /* 保持内边距 */
            transition: opacity 0.3s ease;
        }

        /* 移动端center-content布局优化 */
        .center-content {
            position: static; /* 移动端使用静态定位 */
            transform: none; /* 取消变换 */
            width: 100%;
            text-align: center;
            padding: 0 8px; /* 减少左右内边距 */
        }

        .loading .content {
            opacity: 0.6;
        }

        /* 移动端搜索容器样式 */
        .search-container {
            margin-top: 15px; /* 增加上边距，与右上角按钮拉开距离 */
        }

        .search-bar {
            flex-wrap: nowrap;
            max-width: 320px; /* 限制移动端搜索栏宽度 */
            width: 90%; /* 相对宽度 */
            margin: 6px auto 8px auto; /* 居中显示 */
        }

        .search-bar select {
            width: 80px; /* 缩小选择框宽度，参考佬友修改版 */
            flex: 0 0 auto;
            font-size: 12px; /* 减小字体以适应更小宽度 */
        }

        .search-bar input {
            flex: 1;
        }

        .search-bar button {
            flex: 0 0 auto;
        }

        .admin-controls input,
        .admin-controls button {
            height: 36px;
            padding: 0 10px;
            font-size: 14px;
        }

        .category-button {
            flex: 0 0 auto;
            font-size: 12px;
            padding: 5px 12px;
            white-space: nowrap;
            margin: 0 3px; /* 水平间距 */
        }

        .card-container {
            display: grid;
            grid-template-columns: repeat(2, minmax(140px, 1fr));
            column-gap: 20px;
            row-gap: 10px;
            justify-content: center;
            padding: 12px;
            margin: 0 auto;
        }

        .card {
            width: auto;
            max-width: 100%;
            padding: 12px;
            margin: 0;
            border-radius: 8px;
        }

        .card-title {
            font-size: 13px;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            max-width: 100%;
        }

        .card-url {
            font-size: 11px;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
            max-width: 100%;
        }

        .add-remove-controls {
            right: 5px;
            bottom: 150px;
            top: auto;
            transform: none;
            flex-direction: column;
            gap: 15px;
        }

        .round-btn {
            right: 10px;
            display: flex;
            align-items: center;
            justify-content: center;
            width: 36px;
            height: 36px;
            font-size: 20px;
        }

        .floating-button-group {
            bottom: 20px;
            right: 10px;
        }

        .floating-button-group button {
            width: 36px;
            height: 36px;
            font-size: 18px;
        }

        #dialog-box {
            width: 90%;
            max-width: 350px;
            padding: 20px;
        }

        .section-title {
            font-size: 20px;
            min-width: 100px;
        }
    }

    /* 自定义对话框样式 */
    .dialog-overlay {
        display: none;
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background-color: rgba(0, 0, 0, 0.6);
        backdrop-filter: blur(4px);
        justify-content: center;
        align-items: center;
        z-index: 1000;
        animation: fadeIn 0.3s ease;
    }

    .dialog-box {
        background-color: #ffffff;
        padding: 24px;
        border-radius: 12px;
        width: 340px;
        box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
        transform: translateY(-20px);
        animation: slideUp 0.3s ease forwards;
    }

    .dialog-title {
        margin: 0 0 15px 0;
        font-size: 18px;
        color: #333;
    }

    .dialog-content {
        padding: 15px 0;
        margin-bottom: 16px;
        font-size: 16px;
        line-height: 1.5;
        color: #333;
    }

    .dialog-box input[type="text"] {
        width: 100%;
        margin-bottom: 16px;
        padding: 10px 12px;
        border: 1px solid #e0e0e0;
        border-radius: 8px;
        font-size: 14px;
        transition: all 0.2s;
        box-sizing: border-box;
        background-color: #ffffff !important;
    }

    .dialog-box input[type="text"]:focus {
        border-color: #4a90e2 !important;
        outline: none;
        box-shadow: 0 0 0 3px rgba(74, 144, 226, 0.2);
    }

    .dialog-buttons {
        display: flex;
        justify-content: flex-end;
        gap: 10px;
    }

    .dialog-box button {
        padding: 8px 16px;
        border-radius: 6px;
        border: none;
        font-size: 14px;
        cursor: pointer;
        transition: all 0.2s;
    }

    .dialog-confirm-btn {
        background-color: #43b883;
        color: white;
    }

    .dialog-confirm-btn:hover {
        background-color: #3aa876;
    }

    .dialog-cancel-btn {
        background-color: #f0f0f0;
        color: #555;
    }

    .dialog-cancel-btn:hover {
        background-color: #e0e0e0;
    }

    .top-z-index {
        z-index: 9999;
    }

    /* 动画效果 */
    @keyframes fadeIn {
        from { opacity: 0; }
        to { opacity: 1; }
    }

    @keyframes slideUp {
        from {
            transform: translateY(20px);
            opacity: 0;
        }
        to {
            transform: translateY(0);
            opacity: 1;
        }
    }

    /* 暗色主题对话框样式 */
    body.dark-theme .dialog-box {
        background-color: #2d3748;
        box-shadow: 0 10px 25px rgba(0, 0, 0, 0.3);
    }

    body.dark-theme .dialog-title {
        color: #f8f9fa;
    }

    body.dark-theme .dialog-content {
        color: #f8f9fa;
    }

    body.dark-theme .dialog-box input[type="text"] {
        background-color: #3c4658 !important;
        color: #e3e3e3 !important;
        border-color: #4a5568 !important;
    }

    body.dark-theme .dialog-box input[type="text"]:focus {
        border-color: #5a9cec !important;
        box-shadow: 0 0 0 3px rgba(90, 156, 236, 0.3);
    }

    body.dark-theme .dialog-cancel-btn {
        background-color: #4a5568;
        color: #e3e3e3;
    }

    body.dark-theme .dialog-cancel-btn:hover {
        background-color: #3c4658;
    }

    body.dark-theme .dialog-confirm-btn {
        background-color: #5d7fb9;
        color: white;
    }

    body.dark-theme .dialog-confirm-btn:hover {
        background-color: #5473a9;
    }

    /* 加载遮罩样式 */
    #loading-mask {
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background-color: rgba(0,0,0,0.6);
        backdrop-filter: blur(4px);
        z-index: 7000;
        display: flex;
        align-items: center;
        justify-content: center;
    }

    .loading-content {
        background-color: #fff;
        padding: 20px 40px;
        border-radius: 10px;
        text-align: center;
        box-shadow: 0 0 10px #0003;
        font-size: 16px;
        color: #333;
    }

    /* 加载动画 */
    .spinner {
        width: 40px;
        height: 40px;
        border: 4px solid #ccc;
        border-top-color: #3498db;
        border-radius: 50%;
        margin: 0 auto 10px;
        animation: spin 1s linear infinite;
    }

    @keyframes spin {
        to {
            transform: rotate(360deg);
        }
    }

    body.dark-theme .loading-content {
        background-color: #2d3748;
        color: #f8f9fa;
    }

    /* 分类管理按钮样式 */
    .edit-category-btn, .move-category-btn {
        background-color: #43b883;
        color: white;
        border: none;
        padding: 4px 8px;
        margin-left: 8px;
        border-radius: 4px;
        font-size: 12px;
        cursor: pointer;
        transition: all 0.2s;
        display: none;
    }

    .edit-category-btn:hover {
        background-color: #3aa876;
    }

    .move-category-btn {
        background-color: #5d7fb9;
        padding: 4px 6px;
        min-width: 28px;
        display: inline-flex;
        align-items: center;
        justify-content: center;
    }

    .move-category-btn:hover {
        background-color: #5473a9;
    }

    .move-category-btn svg {
        width: 16px;
        height: 16px;
        fill: white;
    }

    .delete-category-btn {
        background-color: #e74c3c;
        color: white;
        border: none;
        padding: 4px 8px;
        margin-left: 8px;
        border-radius: 4px;
        font-size: 12px;
        cursor: pointer;
        transition: all 0.2s;
        display: none;
    }

    .delete-category-btn:hover {
        background-color: #c0392b;
    }

    /* 暗色主题下的分类管理按钮 */
    body.dark-theme .edit-category-btn {
        background-color: #5d7fb9;
    }

    body.dark-theme .edit-category-btn:hover {
        background-color: #5473a9;
    }

    body.dark-theme .move-category-btn {
        background-color: #43b883;
    }

    body.dark-theme .move-category-btn:hover {
        background-color: #3aa876;
    }

    body.dark-theme .delete-category-btn {
        background-color: #e74c3c;
    }

    body.dark-theme .delete-category-btn:hover {
        background-color: #c0392b;
    }

    /* 按钮顺序控制 */
    .add-btn { order: 1; }
    .remove-btn { order: 2; }
    .category-add-btn { order: 3; }
    .category-manage-btn { order: 4; }

    /* 分类管理按钮激活状态 */
    .category-manage-btn.active {
        background-color: #e74c3c;
    }

    .category-manage-btn.active:hover {
        background-color: #c0392b;
    }

    /* 卡片描述样式 */
    .card-tip {
        font-size: 12px;
        color: #666;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
        text-overflow: ellipsis;
        line-height: 14px;
        max-height: 28px;
        margin-top: 5px;
    }

    body.dark-theme .card-tip {
        color: #a0a0a0;
    }

    /* 卡片按钮容器 */
    .card-actions {
        position: absolute;
        top: -12px;
        right: -12px;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 4px;
        z-index: 15;
        height: 24px;
    }

    /* 卡片按钮通用样式 */
    .card-btn {
        position: relative;
        z-index: 1;
        width: 24px;
        height: 24px;
        border: none;
        border-radius: 50%;
        background: #43b883;
        color: white;
        font-size: 12px;
        cursor: pointer;
        display: none;
        align-items: center;
        justify-content: center;
        transition: transform 0.2s, opacity 0.2s, box-shadow 0.2s;
        padding: 0;
        margin: 0;
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
        flex-shrink: 0;
        vertical-align: top;
    }

    .card-btn:hover {
        z-index: 2;
        transform: translateY(-2px);
        box-shadow: 0 4px 8px rgba(0, 0, 0, 0.3);
    }

    .card-btn svg {
        width: 14px;
        height: 14px;
        stroke: currentColor;
        fill: none;
        display: block;
        margin: auto;
    }

    .edit-btn {
        background: #43b883;
    }

    .delete-btn {
        background: #e74c3c;
    }

    body.dark-theme .edit-btn {
        background: #5d7fb9;
    }

    body.dark-theme .delete-btn {
        background: #e74c3c;
    }

    /* 自定义提示框样式 */
    #custom-tooltip {
        position: absolute;
        display: none;
        z-index: 700;
        background: #43b883;
        color: #fff;
        padding: 6px 10px;
        border-radius: 5px;
        font-size: 12px;
        pointer-events: none;
        max-width: 300px;
        white-space: pre-wrap;
        box-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
        transition: opacity 0.2s ease;
    }

    body.dark-theme #custom-tooltip {
        background: #5d7fb9;
        color: #fff;
    }

    /* 卡片悬停效果 */
    @media (hover: hover) and (pointer: fine) {
        .card:hover {
            transform: scale(1.05);
            box-shadow: 0 10px 10px rgba(0, 0, 0, 0.3);
        }

        .card.no-hover:hover {
            transform: none !important;
            box-shadow: 0 5px 5px rgba(0, 0, 0, 0.2) !important;
        }

        body.dark-theme .card.no-hover:hover {
            transform: none !important;
            box-shadow: 0 5px 5px rgba(0, 0, 0, 0.2) !important;
        }
    }


    </style>
</head>

<body>
    <div class="ambient-background" aria-hidden="true">
        <div class="ambient-grid"></div>
        <div class="ambient-orb orb-a"></div>
        <div class="ambient-orb orb-b"></div>
        <div class="ambient-orb orb-c"></div>
    </div>
    <div class="fixed-elements">
        <h3><span class="weather-mini" id="weather-mini" onclick="openWeatherModal()"><span class="weather-loading">天气</span></span></h3>
        <div class="center-content">
            <!-- 一言模块 -->
            <p id="hitokoto">
                <a href="#" id="hitokoto_text"></a>
            </p>
            <script src="https://v1.hitokoto.cn/?encode=js&select=%23hitokoto" defer></script>
            <!-- 搜索栏 -->
            <div class="search-container">
                <div class="search-bar">
                    <select id="search-engine-select">
                        <option value="baidu">百度</option>
                        <option value="bing">必应</option>
                        <option value="google">谷歌</option>
                        <option value="duckduckgo">DuckDuckGo</option>
                    </select>
                    <input type="text" id="search-input" placeholder="">
                    <button id="search-button">🔍</button>
                </div>
            </div>
            <div id="category-buttons-container" class="category-buttons-container"></div>
        </div>
        <!-- 右上角控制区域 -->
        <div class="top-right-controls">
            <button class="admin-btn" id="admin-btn" onclick="toggleAdminMode()" style="display: none;">设置</button>
            <button class="login-btn" id="login-btn" onclick="handleLoginClick()">登录</button>
            <button class="github-btn has-tooltip tooltip-bottom tooltip-green" onclick="openGitHub()" data-tooltip="喜欢请点个star">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/>
                </svg>
            </button>
            <div class="bookmark-search-toggle" onclick="toggleBookmarkSearch()">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                    <circle cx="11" cy="11" r="8"></circle>
                    <path d="m21 21-4.35-4.35"></path>
                    <line x1="11" y1="8" x2="11" y2="14"></line>
                    <line x1="8" y1="11" x2="14" y2="11"></line>
                </svg>
                <div class="bookmark-search-dropdown" id="bookmark-search-dropdown">
                    <input type="text" id="bookmark-search-input" placeholder="搜索书签...">
                </div>
            </div>
        </div>
    </div>
    <div class="content">
        <!-- 管理控制按钮 -->
        <div class="add-remove-controls">
            <button class="round-btn add-btn" onclick="showAddDialog()" title="添加链接">
                <svg viewBox="0 0 48 48" width="24" height="24" xmlns="http://www.w3.org/2000/svg">
                    <path d="M16 6H8a2 2 0 0 0-2 2v8M16 42H8a2 2 0 0 1-2-2v-8M32 42h8a2 2 0 0 0 2-2v-8M32 6h8a2 2 0 0 1 2 2v8" stroke="white" stroke-width="4" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
                    <path d="M32 24H16M24 16v16" stroke="white" stroke-width="4" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
                </svg>
            </button>

            <button class="round-btn remove-btn" onclick="toggleRemoveMode()" title="编辑链接">
                <svg viewBox="0 0 48 48" width="24" height="24" xmlns="http://www.w3.org/2000/svg">
                    <path d="M42 26v14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h14" stroke="white" stroke-width="4" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
                    <path d="M14 26.72V34h7.32L42 13.31 34.7 6 14 26.72Z" stroke="white" stroke-width="4" stroke-linejoin="round" fill="none"/>
                </svg>
            </button>

            <button class="round-btn category-add-btn" onclick="addCategory()" title="添加分类">
                <svg viewBox="0 0 48 48" width="24" height="24" xmlns="http://www.w3.org/2000/svg">
                    <path d="M5 8c0-1.1.9-2 2-2h12l5 6h17c1.1 0 2 .9 2 2v26c0 1.1-.9 2-2 2H7c-1.1 0-2-.9-2-2V8Z" stroke="white" stroke-width="4" stroke-linejoin="round" fill="none"/>
                    <path d="M18 27h12M24 21v12" stroke="white" stroke-width="4" stroke-linecap="round"/>
                </svg>
            </button>

            <button class="round-btn category-manage-btn" onclick="toggleEditCategory()" title="编辑分类">
                <svg viewBox="0 0 48 48" width="24" height="24" xmlns="http://www.w3.org/2000/svg">
                    <path d="M5 8c0-1.1.9-2 2-2h12l5 6h17c1.1 0 2 .9 2 2v26c0 1.1-.9 2-2 2H7c-1.1 0-2-.9-2-2V8Z" stroke="white" stroke-width="4" stroke-linejoin="round" fill="none"/>
                    <circle cx="24" cy="28" r="4" stroke="white" stroke-width="4" fill="none"/>
                    <path d="M24 21v3m0 8v3m4.8-12-2.1 2.1M20.8 31l-2.1 2.1M19 23l2.1 2.1M27 31l2.1 2.1M17 28h3M28 28h3" stroke="white" stroke-width="4" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
            </button>
        </div>



        <!-- 分类和卡片容器 -->
        <div id="boot-status" class="boot-status">页面正在加载书签数据。如果这里长时间不变化，通常是书签接口或 KV 绑定没有返回数据。</div>
        <div id="sections-container"></div>
        <!-- 浮动按钮组 -->
        <div class="floating-button-group">
            <button id="back-to-top-btn" onclick="scrollToTop()" style="display: none;">
                <svg width="24" height="24" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M12 24l12-12 12 12m-24 12 12-12 12 12" stroke="#fff" stroke-width="4" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
            </button>
            <button id="theme-toggle" onclick="toggleTheme()" title="主题模式：自动">自</button>
        </div>
        <!-- 添加链接对话框 -->
        <div id="dialog-overlay">
            <div id="dialog-box">
                <label for="name-input">名称</label>
                <input type="text" id="name-input" placeholder="必填">
                <label for="url-input">地址</label>
                <input type="text" id="url-input" placeholder="必填">
                <label for="tips-input">描述</label>
                <input type="text" id="tips-input" placeholder="可选">
                <label for="icon-input">图标</label>
                <input type="text" id="icon-input" placeholder="可选">
                <label for="category-select">选择分类</label>
                <select id="category-select"></select>
                <div class="private-link-container">
                    <label for="private-checkbox">私密链接</label>
                    <input type="checkbox" id="private-checkbox">
                </div>
                <div class="dialog-buttons">
                    <button class="dialog-cancel-btn" id="dialog-cancel-btn">取消</button>
                    <button class="dialog-confirm-btn" id="dialog-confirm-btn">确定</button>
                </div>
            </div>
        </div>
        <!-- 登录弹窗 -->
        <div id="login-modal" class="login-modal">
            <div class="login-modal-content">
                <h3>登录</h3>
                <input type="password" id="login-password" placeholder="请输入密码">
                <div class="login-modal-buttons">
                    <button class="cancel" onclick="hideLoginModal()">取消</button>
                    <button onclick="performLogin()">确定</button>
                </div>
            </div>
        </div>

        <!-- 自定义Alert对话框 -->
        <div class="dialog-overlay top-z-index" id="custom-alert-overlay" style="display: none;">
            <div class="dialog-box" id="custom-alert-box">
                <h3 class="dialog-title" id="custom-alert-title">提示</h3>
                <div class="dialog-content" id="custom-alert-content">这里是提示内容</div>
                <div class="dialog-buttons">
                    <button class="dialog-confirm-btn" id="custom-alert-confirm">确定</button>
                </div>
            </div>
        </div>

        <!-- 自定义Confirm对话框 -->
        <div class="dialog-overlay top-z-index" id="custom-confirm-overlay" style="display: none;">
            <div class="dialog-box">
                <div class="dialog-content" id="custom-confirm-message"></div>
                <div class="dialog-buttons">
                    <button id="custom-confirm-cancel" class="dialog-cancel-btn">取消</button>
                    <button id="custom-confirm-ok" class="dialog-confirm-btn">确定</button>
                </div>
            </div>
        </div>

        <!-- 分类名称输入对话框 -->
        <div class="dialog-overlay" id="category-dialog" style="display: none;">
            <div class="dialog-box">
                <h3 id="category-dialog-title" class="dialog-title">新建分类</h3>
                <input type="text" id="category-name-input" class="category-dialog-input" placeholder="请输入分类名称">
                <div class="dialog-buttons">
                    <button id="category-cancel-btn" class="dialog-cancel-btn">取消</button>
                    <button id="category-confirm-btn" class="dialog-confirm-btn">确定</button>
                </div>
            </div>
        </div>

        <!-- 加载遮罩 -->
        <div id="loading-mask" style="display:none;">
            <div class="loading-content">
                <div class="spinner"></div>
                <p>加载中，请稍候...</p>
            </div>
        </div>
    </div>
    <div id="custom-tooltip"></div>

    <script>
    // 搜索引擎配置
    const searchEngines = {
        baidu: "https://www.baidu.com/s?wd=",
        bing: "https://www.bing.com/search?q=",
        google: "https://www.google.com/search?q=",
        duckduckgo: "https://duckduckgo.com/?q="
    };

    let currentEngine = "baidu";
    let mainAppBooted = false;
    let weatherAppBooted = false;
    let ambientBackgroundBooted = false;
    let bootStatusPinned = false;

    // 日志记录函数
    function logAction(action, details) {
        const timestamp = new Date().toISOString();
        const logEntry = timestamp + ': ' + action + ' - ' + JSON.stringify(details);
        console.log(logEntry);
    }

    function setBootStatus(message, isError) {
        const statusEl = document.getElementById('boot-status');
        if (!statusEl) return;
        statusEl.textContent = message;
        statusEl.classList.toggle('error', !!isError);
        statusEl.style.display = 'block';
        bootStatusPinned = !!isError;
    }

    function hideBootStatus() {
        const statusEl = document.getElementById('boot-status');
        if (!statusEl) return;
        statusEl.style.display = 'none';
        statusEl.classList.remove('error');
        bootStatusPinned = false;
    }

    async function fetchWithTimeout(resource, options, timeoutMs) {
        const controller = new AbortController();
        const timer = setTimeout(function() {
            controller.abort();
        }, timeoutMs || 8000);

        try {
            const finalOptions = Object.assign({}, options || {}, { signal: controller.signal });
            return await fetch(resource, finalOptions);
        } finally {
            clearTimeout(timer);
        }
    }

    window.addEventListener('error', function(event) {
        setBootStatus('前端脚本执行失败：' + event.message + '。请检查 Worker 控制台日志。', true);
    });

    window.addEventListener('unhandledrejection', function(event) {
        const reason = event.reason && event.reason.message ? event.reason.message : String(event.reason || '未知错误');
        setBootStatus('页面初始化失败：' + reason, true);
    });

    function getRandomNumber(min, max) {
        return Math.random() * (max - min) + min;
    }

    function animateAmbientBackground() {
        const palette = [
            ['rgba(67, 184, 131, 0.34)', 'rgba(139, 214, 171, 0.18)'],
            ['rgba(93, 127, 185, 0.28)', 'rgba(142, 168, 214, 0.16)'],
            ['rgba(255, 194, 102, 0.2)', 'rgba(250, 138, 98, 0.14)']
        ];

        document.querySelectorAll('.ambient-orb').forEach(function(orb, index) {
            const x = getRandomNumber(-6, 72);
            const y = getRandomNumber(-8, 68);
            const scale = getRandomNumber(0.86, 1.34);
            const opacity = getRandomNumber(0.22, 0.52);
            const colors = palette[index % palette.length];

            orb.style.transform = 'translate3d(' + x + 'vw, ' + y + 'vh, 0) scale(' + scale.toFixed(2) + ')';
            orb.style.opacity = opacity.toFixed(2);
            orb.style.background = 'radial-gradient(circle, ' + colors[0] + ' 0%, ' + colors[1] + ' 52%, rgba(255,255,255,0) 74%)';
        });
    }

    function initAmbientBackground() {
        if (ambientBackgroundBooted) return;
        ambientBackgroundBooted = true;
        animateAmbientBackground();
        setInterval(animateAmbientBackground, 12000);
    }

    // 设置当前搜索引擎
    function setActiveEngine(engine) {
        currentEngine = engine;
        document.getElementById('search-engine-select').value = engine;
        logAction('设置搜索引擎', { engine });
    }

    // 搜索引擎选择框变更事件
    document.getElementById('search-engine-select').addEventListener('change', function() {
        setActiveEngine(this.value);
    });

    // 搜索按钮点击事件
    document.getElementById('search-button').addEventListener('click', () => {
        const query = document.getElementById('search-input').value;
        if (query) {
            logAction('执行搜索', { engine: currentEngine, query });
            window.open(searchEngines[currentEngine] + encodeURIComponent(query), '_blank');
        }
    });

    // 搜索输入框回车事件
    document.getElementById('search-input').addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            document.getElementById('search-button').click();
        }
    });

    // 初始化搜索引擎
    setActiveEngine(currentEngine);

    // 全局变量
    let publicLinks = [];
    let privateLinks = [];
    let isAdmin = false;
    let isLoggedIn = false;
    let removeMode = false;
    let isRemoveCategoryMode = false;
    let isEditCategoryMode = false;
    let isDarkTheme = false;
    let currentThemeMode = 'auto';
    let themeMediaQuery = null;
    let themeMediaListenerBound = false;
    let themeVisibilityListenerBound = false;
    let themeAutoRefreshTimer = null;
    let links = [];
    const categories = {};
    const THEME_MODE_KEY = 'card_tab_theme_mode';

    // 添加新分类
    async function addCategory() {
        if (!await validateToken()) {
            return;
        }
        const categoryName = await showCategoryDialog('请输入新分类名称');
        if (categoryName && !categories[categoryName]) {
            categories[categoryName] = [];
            updateCategorySelect();
            renderSections();
            saveLinks();
            logAction('添加分类', { categoryName, currentLinkCount: links.length });
        } else if (categories[categoryName]) {
            await customAlert('该分类已存在', '添加分类');
            logAction('添加分类失败', { categoryName, reason: '分类已存在' });
        }
    }

    // 删除分类
    async function deleteCategory(category) {
        if (!await validateToken()) {
            return;
        }
        const message = '确定要删除 "' + category + '" 分类吗？这将删除该分类下的所有链接。';
        const confirmed = await customConfirm(message, '确定', '取消');

        if (confirmed) {
            delete categories[category];
            links = links.filter(link => link.category !== category);
            publicLinks = publicLinks.filter(link => link.category !== category);
            privateLinks = privateLinks.filter(link => link.category !== category);
            updateCategorySelect();
            renderSections();
            renderCategoryButtons();
            saveLinks();
            logAction('删除分类', { category });
        }
    }

    // 编辑分类名称
    async function editCategoryName(oldName) {
        if (!await validateToken()) return;

        const newName = await showCategoryDialog('请输入新的分类名称', oldName);
        if (!newName || newName === oldName) return;

        if (categories[newName]) {
            await customAlert('该名称已存在，请重新命名', '编辑分类');
            return;
        }

        // 1. 重命名分类对象
        categories[newName] = categories[oldName];
        delete categories[oldName];

        // 2. 更新所有链接的 category 字段
        [...publicLinks, ...privateLinks].forEach(link => {
            if (link.category === oldName) {
                link.category = newName;
            }
        });

        links.forEach(link => {
            if (link.category === oldName) {
                link.category = newName;
            }
        });

        // 3. 保存并刷新
        renderSections();
        renderCategoryButtons();
        updateCategorySelect();
        saveLinks();

        logAction('编辑分类名称', { oldName, newName });
    }

    // 移动分类
    async function moveCategory(categoryName, direction) {
        if (!await validateToken()) {
            return;
        }
        const keys = Object.keys(categories);
        const index = keys.indexOf(categoryName);
        if (index < 0) return;

        const newIndex = index + direction;
        if (newIndex < 0 || newIndex >= keys.length) return;

        // 重建一个新顺序的 categories 对象
        const newCategories = {};
        const reordered = [...keys];
        [reordered[index], reordered[newIndex]] = [reordered[newIndex], reordered[index]];
        reordered.forEach(key => {
            newCategories[key] = categories[key];
        });

        // 替换原有 categories 并重渲染
        Object.keys(categories).forEach(k => delete categories[k]);
        Object.assign(categories, newCategories);

        renderSections();
        renderCategoryButtons();
        updateCategorySelect();
        saveLinks();

        logAction('移动分类', { categoryName, direction });
    }

    // 切换分类编辑模式
    function toggleEditCategory() {
        isEditCategoryMode = !isEditCategoryMode;

        const deleteButtons = document.querySelectorAll('.delete-category-btn');
        const editButtons = document.querySelectorAll('.edit-category-btn');
        const moveButtons = document.querySelectorAll('.move-category-btn');

        deleteButtons.forEach(btn => {
            btn.style.display = isEditCategoryMode ? 'inline-block' : 'none';
        });

        editButtons.forEach(btn => {
            btn.style.display = isEditCategoryMode ? 'inline-block' : 'none';
        });

        moveButtons.forEach(btn => {
            btn.style.display = isEditCategoryMode ? 'inline-block' : 'none';
        });

        // 更新分类管理按钮的样式以显示当前状态
        const manageButton = document.querySelector('.category-manage-btn');
        if (manageButton) {
            if (isEditCategoryMode) {
                manageButton.classList.add('active');
            } else {
                manageButton.classList.remove('active');
            }
        }

        // 给用户提示 - 暂时使用console.log避免阻塞
        if (isEditCategoryMode) {
            console.log('分类编辑模式已开启');
        } else {
            console.log('分类编辑模式已关闭');
        }

        logAction('切换分类编辑模式', { isEditCategoryMode });
    }



    // 渲染分类快捷按钮
    function renderCategoryButtons() {
        // 如果正在显示搜索结果，不重新渲染分类按钮
        if (isShowingSearchResults) {
            return;
        }

        const buttonsContainer = document.getElementById('category-buttons-container');
        buttonsContainer.innerHTML = '';

        // 只有当有分类时才显示按钮容器
        if (Object.keys(categories).length > 0) {
            // 获取页面上实际显示的分类顺序（只从sections-container中获取，不包括搜索结果）
            const displayedCategories = [];
            document.querySelectorAll('#sections-container .section-title').forEach(titleElement => {
                displayedCategories.push(titleElement.textContent);
            });

            // 创建按钮并添加到容器
            let visibleButtonsCount = 0;
            displayedCategories.forEach(category => {
                // 检查该分类是否有可见的链接
                const visibleLinks = links.filter(function(link) {
                    return link.category === category && (!link.isPrivate || isLoggedIn);
                });

                // 只为有可见链接的分类创建按钮
                if (visibleLinks.length > 0) {
                    const button = document.createElement('button');
                    button.className = 'category-button';
                    button.textContent = category;
                    button.dataset.category = category;
                    button.onclick = () => {
                        // 如果正在显示搜索结果，先隐藏搜索结果
                        if (isShowingSearchResults) {
                            hideSearchResults();
                        }

                        // 清除所有按钮的active类
                        document.querySelectorAll('.category-button').forEach(btn => {
                            btn.classList.remove('active');
                        });
                        // 为当前点击的按钮添加active类
                        button.classList.add('active');
                        scrollToCategory(category);
                    };

                    buttonsContainer.appendChild(button);
                    visibleButtonsCount++;
                }
            });

            // 显示或隐藏按钮容器
            if (visibleButtonsCount > 0) {
                buttonsContainer.style.display = 'flex';
            } else {
                buttonsContainer.style.display = 'none';
            }

            // 初始时检测当前可见分类并设置相应按钮为活跃状态
            setTimeout(setActiveCategoryButtonByVisibility, 100);
        } else {
            buttonsContainer.style.display = 'none';
        }
    }

    // 根据可见性设置活跃的分类按钮
    function setActiveCategoryButtonByVisibility() {
        // 如果正在显示搜索结果，不更新分类按钮的活跃状态
        if (isShowingSearchResults) {
            return;
        }

        // 获取所有分类区域
        const sections = document.querySelectorAll('.section');
        if (!sections.length) return;

        // 获取视窗高度
        const viewportHeight = window.innerHeight;
        // 考虑固定元素的高度
        const fixedElementsHeight = 170;
        // 计算视窗中心点
        const viewportCenter = viewportHeight / 2 + fixedElementsHeight;

        // 找出最接近视窗中心的分类
        let closestSection = null;
        let closestDistance = Infinity;

        sections.forEach(section => {
            const rect = section.getBoundingClientRect();
            // 计算分类区域的中心点
            const sectionCenter = rect.top + rect.height / 2;
            // 计算到视窗中心的距离
            const distance = Math.abs(sectionCenter - viewportCenter);

            if (distance < closestDistance) {
                closestDistance = distance;
                closestSection = section;
            }
        });

        if (closestSection) {
            const cardContainer = closestSection.querySelector('.card-container');
            if (cardContainer && cardContainer.id) {
                const categoryId = cardContainer.id;
                const buttons = document.querySelectorAll('.category-button');

                // 移除所有活跃状态
                buttons.forEach(btn => btn.classList.remove('active'));

                // 为匹配的分类按钮添加活跃状态
                buttons.forEach(btn => {
                    if (btn.dataset.category === categoryId) {
                        btn.classList.add('active');
                    }
                });
            }
        }
    }

    // 添加滚动事件监听器，滚动时更新活跃的分类按钮
    window.addEventListener('scroll', debounce(setActiveCategoryButtonByVisibility, 100));

    // 防抖函数，避免过多的滚动事件处理
    function debounce(func, wait) {
        let timeout;
        return function() {
            const context = this;
            const args = arguments;
            clearTimeout(timeout);
            timeout = setTimeout(() => {
                func.apply(context, args);
            }, wait);
        };
    }

    // 滚动到指定分类
    function scrollToCategory(category) {
        const section = document.getElementById(category);
        if (section) {
            // 计算滚动位置，考虑顶部固定元素的高度和额外偏移量
            let offset = 230; // 减小偏移量，确保分类标题和第一行书签完全可见

            // 检查是否为移动设备
            if (window.innerWidth <= 480) {
                offset = 120; // 移动设备上的偏移量
            }

            // 滚动到分类位置
            const sectionRect = section.getBoundingClientRect();
            const absoluteTop = window.pageYOffset + sectionRect.top - offset;

            // 使用平滑滚动效果
            window.scrollTo({
                top: absoluteTop,
                behavior: 'smooth'
            });

            logAction('滚动到分类', { category });
        }
    }

    // 读取链接数据
    async function loadLinks() {
        setBootStatus('正在读取书签数据...', false);
        const headers = {
            'Content-Type': 'application/json'
        };

        // 如果已登录，从 localStorage 获取 token 并添加到请求头
        if (isLoggedIn) {
            const token = localStorage.getItem('authToken');
            if (token) {
                headers['Authorization'] = token;
            }
        }

        try {
            const response = await fetchWithTimeout('/api/getLinks?userId=testUser', {
                headers: headers
            }, 8000);

            if (!response.ok) {
                throw new Error("HTTP error! status: " + response.status);
            }


            const data = await response.json();
            console.log('Received data:', data);

            if (data.warning === 'card_order_unbound') {
                setBootStatus('未检测到 CARD_ORDER KV 绑定，当前先按空数据展示。请在 Cloudflare Worker 中重新绑定 CARD_ORDER。', true);
            } else if (data.warning === 'card_order_get_timeout') {
                setBootStatus('CARD_ORDER 读取超时，当前先按空数据展示。请检查 KV 服务状态或稍后重试。', true);
            } else if (data.warning === 'card_order_invalid_json') {
                setBootStatus('CARD_ORDER 中的数据格式不合法，当前先按空数据展示。', true);
            }

            if (data.categories) {
                Object.assign(categories, data.categories);
            }

            publicLinks = data.links ? data.links.filter(link => !link.isPrivate) : [];
            privateLinks = data.links ? data.links.filter(link => link.isPrivate) : [];
            links = isLoggedIn ? [...publicLinks, ...privateLinks] : publicLinks;

            renderSections();
            updateCategorySelect();
            updateUIState();
            logAction('读取链接', {
                publicCount: publicLinks.length,
                privateCount: privateLinks.length,
                isLoggedIn: isLoggedIn,
                hasToken: !!localStorage.getItem('authToken')
            });
        } catch (error) {
            // 🔧 安全修复：避免泄露详细错误信息
            console.error('Failed to load links');
            console.error('加载链接时出错，请刷新页面重试');
            const message = error && error.name === 'AbortError'
                ? '书签接口请求超时。请检查 Worker 是否已绑定 CARD_ORDER KV，或接口是否被其他请求卡住。'
                : '书签数据读取失败。请检查 CARD_ORDER KV 绑定、接口返回状态，或确认当前用户下已有数据。';
            setBootStatus(message, true);
        }
    }


    // 更新UI状态
    function updateUIState() {
        const addRemoveControls = document.querySelector('.add-remove-controls');

        if (isAdmin) {
            addRemoveControls.style.display = 'flex';
        } else {
            addRemoveControls.style.display = 'none';
        }

        // 同时更新登录和设置按钮状态
        updateLoginButton();

        logAction('更新UI状态', { isAdmin, isLoggedIn });
    }

    // 登录状态显示（加载所有链接）
    function showSecretGarden() {
        if (isLoggedIn) {
            links = [...publicLinks, ...privateLinks];
            renderSections();
            // 显示所有私密标签
            document.querySelectorAll('.private-tag').forEach(tag => {
                tag.style.display = 'block';
            });
            logAction('显示私密花园');
        }
    }

    // 渲染分类和链接
    function renderSections() {
        const container = document.getElementById('sections-container');
        container.innerHTML = '';

        Object.keys(categories).forEach(category => {
            const section = document.createElement('div');
            section.className = 'section';

            const titleContainer = document.createElement('div');
            titleContainer.className = 'section-title-container';

            const title = document.createElement('div');
            title.className = 'section-title';
            title.textContent = category;

            titleContainer.appendChild(title);

            if (isAdmin) {
                const editBtn = document.createElement('button');
                editBtn.textContent = '编辑名称';
                editBtn.className = 'edit-category-btn';
                editBtn.style.display = isEditCategoryMode ? 'inline-block' : 'none';
                editBtn.onclick = () => editCategoryName(category);
                titleContainer.appendChild(editBtn);

                const deleteBtn = document.createElement('button');
                deleteBtn.textContent = '删除分类';
                deleteBtn.className = 'delete-category-btn';
                deleteBtn.style.display = isEditCategoryMode ? 'inline-block' : 'none';
                deleteBtn.onclick = () => deleteCategory(category);
                titleContainer.appendChild(deleteBtn);

                const upBtn = document.createElement('button');
                upBtn.innerHTML = '<svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor"><path d="M12 6l-6 6h4v6h4v-6h4z"/></svg>';
                upBtn.className = 'move-category-btn';
                upBtn.style.display = isEditCategoryMode ? 'inline-block' : 'none';
                upBtn.onclick = () => moveCategory(category, -1);
                titleContainer.appendChild(upBtn);

                const downBtn = document.createElement('button');
                downBtn.innerHTML = '<svg width="22" height="22" viewBox="0 0 24 24" fill="currentColor"><path d="M12 18l6-6h-4v-6h-4v6h-4z"/></svg>';
                downBtn.className = 'move-category-btn';
                downBtn.style.display = isEditCategoryMode ? 'inline-block' : 'none';
                downBtn.onclick = () => moveCategory(category, 1);
                titleContainer.appendChild(downBtn);
            }

            const cardContainer = document.createElement('div');
            cardContainer.className = 'card-container';
            cardContainer.id = category;

            section.appendChild(titleContainer);
            section.appendChild(cardContainer);

            let privateCount = 0;
            let linkCount = 0;

            links.forEach(link => {
                if (link.category === category) {
                    if (link.isPrivate) privateCount++;
                    linkCount++;
                    createCard(link, cardContainer);
                }
            });

            if (privateCount < linkCount || isLoggedIn) {
                container.appendChild(section);
            }
        });

        // 渲染分类快捷按钮
        renderCategoryButtons();

        if (!container.children.length) {
            if (bootStatusPinned) {
                return;
            }
            if (links.length || Object.keys(categories).length) {
                setBootStatus('当前分类下没有可显示的书签。可能是书签都被标记为私密，登录后才会显示。', false);
            } else {
                setBootStatus('当前没有书签数据。你可以先进入设置添加书签，或者检查 KV 中是否已有数据。', false);
            }
        } else {
            hideBootStatus();
        }

        logAction('渲染分类和链接', { isAdmin: isAdmin, linkCount: links.length, categoryCount: Object.keys(categories).length });
    }

    // 从URL中提取域名
    function extractDomain(url) {
        let domain;
        try {
            domain = new URL(url).hostname;
        } catch (e) {
            domain = url;
        }
        return domain;
    }

    // URL验证函数
    function isValidUrl(url) {
        try {
            new URL(url);
            return true;
        } catch {
            return false;
        }
    }

    // URL规范化函数
    function normalizeUrl(url) {
        if (!url || typeof url !== 'string') {
            return url;
        }

        // 去除首尾空格
        url = url.trim();

        // 补全协议（如果缺少）
        if (!/^https?:\\/\\//i.test(url)) {
            url = 'https://' + url;
        }

        try {
            const parsed = new URL(url);

            // 协议小写
            let normalized = parsed.protocol.toLowerCase() + '//';

            // 域名小写
            normalized += parsed.hostname.toLowerCase();

            // 移除默认端口（80 for http, 443 for https）
            if (parsed.port &&
                !((parsed.protocol === 'http:' && parsed.port === '80') ||
                  (parsed.protocol === 'https:' && parsed.port === '443'))) {
                normalized += ':' + parsed.port;
            }

            // 路径保留原样（服务器可能区分大小写）
            // 但移除根路径的单斜杠
            if (parsed.pathname && parsed.pathname !== '/') {
                normalized += parsed.pathname;
            }

            // 保留查询参数
            if (parsed.search) {
                normalized += parsed.search;
            }

            // 不保留 fragment（#hash 部分）

            return normalized;
        } catch {
            // URL 解析失败，返回小写处理后的原始值
            return url.toLowerCase();
        }
    }

    // 创建卡片
    function createCard(link, container) {
        const card = document.createElement('div');
        card.className = 'card';
        card.setAttribute('draggable', isAdmin);
        card.dataset.isPrivate = link.isPrivate;
        card.setAttribute('data-url', link.url);

        // 设置卡片动画延迟
        const cardIndex = container.children.length;
        card.style.setProperty('--card-index', cardIndex);

        const cardTop = document.createElement('div');
        cardTop.className = 'card-top';

        // 定义默认的 SVG 图标
        const defaultIconSVG = '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">' +
        '<path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71"></path>' +
        '<path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"></path>' +
        '</svg>';

        // 创建图标元素
        const icon = document.createElement('img');
        icon.className = 'card-icon';

        // 使用自定义图标或回退到favicon提取服务
        icon.src = (
            !link.icon ||
            typeof link.icon !== 'string' ||
            !link.icon.trim() ||
            !isValidUrl(link.icon)
        )
            ? 'https://www.faviconextractor.com/favicon/' + extractDomain(link.url)
            : link.icon;

        icon.alt = 'Website Icon';

        // 如果图片加载失败，使用默认的 SVG 图标
        icon.onerror = function() {
            const svgBlob = new Blob([defaultIconSVG], {type: 'image/svg+xml'});
            const svgUrl = URL.createObjectURL(svgBlob);
            this.src = svgUrl;

            this.onload = () => URL.revokeObjectURL(svgUrl);
        };

        const title = document.createElement('div');
        title.className = 'card-title';
        title.textContent = link.name;

        cardTop.appendChild(icon);
        cardTop.appendChild(title);

        const url = document.createElement('div');
        url.className = 'card-url';
        url.textContent = link.url;

        card.appendChild(cardTop);
        card.appendChild(url);

        if (link.isPrivate) {
            const privateTag = document.createElement('div');
            privateTag.className = 'private-tag';
            privateTag.textContent = '私密';
            card.appendChild(privateTag);
        }

        const correctedUrl = link.url.startsWith('http://') || link.url.startsWith('https://') ? link.url : 'http://' + link.url;

        if (!isAdmin) {
            card.addEventListener('click', () => {
                window.open(correctedUrl, '_blank');
                logAction('打开链接', { name: link.name, url: correctedUrl });
            });
        }

        // 创建按钮容器
        const cardActions = document.createElement('div');
        cardActions.className = 'card-actions';

        // 编辑按钮
        const editBtn = document.createElement('button');
        editBtn.className = 'card-btn edit-btn';
        editBtn.innerHTML = '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">' +
            '<path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>' +
            '<path d="m18.5 2.5 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>' +
            '</svg>';
        editBtn.title = '编辑';
        editBtn.onclick = function (event) {
            event.stopPropagation();
            showEditDialog(link);
        };

        // 删除按钮
        const deleteBtn = document.createElement('button');
        deleteBtn.className = 'card-btn delete-btn';
        deleteBtn.innerHTML = '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">' +
            '<polyline points="3,6 5,6 21,6"></polyline>' +
            '<path d="m19,6v14a2,2 0 0,1 -2,2H7a2,2 0 0,1 -2,-2V6m3,0V4a2,2 0 0,1 2,-2h4a2,2 0 0,1 2,2v2"></path>' +
            '<line x1="10" y1="11" x2="10" y2="17"></line>' +
            '<line x1="14" y1="11" x2="14" y2="17"></line>' +
            '</svg>';
        deleteBtn.title = '删除';
        deleteBtn.onclick = function (event) {
            event.stopPropagation();
            removeCard(card);
        };

        cardActions.appendChild(editBtn);
        cardActions.appendChild(deleteBtn);
        card.appendChild(cardActions);

        // 添加鼠标悬停事件处理描述提示
        card.addEventListener('mousemove', (e) => handleTooltipMouseMove(e, link.tips, isAdmin));
        card.addEventListener('mouseleave', handleTooltipMouseLeave);

        card.addEventListener('dragstart', dragStart);
        card.addEventListener('dragover', dragOver);
        card.addEventListener('dragend', dragEnd);
        card.addEventListener('drop', drop);
        card.addEventListener('touchstart', touchStart, { passive: false });

        if (isAdmin && removeMode) {
            editBtn.style.display = 'flex';
            deleteBtn.style.display = 'flex';
        }

        if (isAdmin || (link.isPrivate && isLoggedIn) || !link.isPrivate) {
            container.appendChild(card);
        }

    }



    // 更新分类选择下拉框
    function updateCategorySelect() {
        const categorySelect = document.getElementById('category-select');
        categorySelect.innerHTML = '';

        Object.keys(categories).forEach(category => {
            const option = document.createElement('option');
            option.value = category;
            option.textContent = category;
            categorySelect.appendChild(option);
        });

        logAction('更新分类选择', { categoryCount: Object.keys(categories).length });
    }

    // 保存链接数据
    async function saveLinks() {
        if (isAdmin && !(await validateToken())) {
            return;
        }

        let allLinks = [...publicLinks, ...privateLinks];

        // 懒迁移：保存前规范化所有 URL
        allLinks = allLinks.map(link => ({
            ...link,
            url: normalizeUrl(link.url)
        }));

        try {
            await fetch('/api/saveOrder', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': localStorage.getItem('authToken')
                },
                body: JSON.stringify({
                    userId: 'testUser',
                    links: allLinks,
                    categories: categories
                }),
            });
            logAction('保存链接', { linkCount: allLinks.length, categoryCount: Object.keys(categories).length });
        } catch (error) {
            // 🔧 安全修复：避免泄露详细错误信息
            logAction('保存链接失败', { error: 'Save operation failed' });
            console.error('保存链接失败，请重试');
        }
    }

    // 添加卡片弹窗
    async function addLink() {
        if (!await validateToken()) {
            return;
        }
        const name = document.getElementById('name-input').value.trim();
        const url = document.getElementById('url-input').value.trim();
        const tips = document.getElementById('tips-input').value.trim();
        const icon = document.getElementById('icon-input').value.trim();
        const category = document.getElementById('category-select').value;
        const isPrivate = document.getElementById('private-checkbox').checked;

        // 验证必填字段
        if (!name || !url || !category) {
            let errorMessage = '';
            if (!name && !url) {
                errorMessage = '请输入名称和URL';
            } else if (!name) {
                errorMessage = '请输入名称';
            } else if (!url) {
                errorMessage = '请输入URL';
            }

            await customAlert(errorMessage, '添加卡片');
            if (!name) {
                document.getElementById('name-input').focus();
            } else if (!url) {
                document.getElementById('url-input').focus();
            }
            return;
        }

        // 规范化 URL 并检查是否已存在
        const normalizedUrl = normalizeUrl(url);
        const allLinks = [...publicLinks, ...privateLinks];
        // 预处理：一次性规范化所有 URL（性能优化）
        const existingUrls = new Set(allLinks.map(link => normalizeUrl(link.url)));

        if (existingUrls.has(normalizedUrl)) {
            await customAlert('该URL已存在，请勿重复添加', '添加卡片');
            document.getElementById('url-input').focus();
            return;
        }

        // 存储规范化后的 URL
        const newLink = { name, url: normalizedUrl, tips, icon, category, isPrivate };

        if (isPrivate) {
            privateLinks.push(newLink);
        } else {
            publicLinks.push(newLink);
        }

        links = isLoggedIn ? [...publicLinks, ...privateLinks] : publicLinks;

        if (isAdmin || (isPrivate && isLoggedIn) || !isPrivate) {
            const container = document.getElementById(category);
            if (container) {
                createCard(newLink, container);
            } else {
                categories[category] = [];
                renderSections();
            }
        }

        saveLinks();

        // 清空表单
        document.getElementById('name-input').value = '';
        document.getElementById('url-input').value = '';
        document.getElementById('tips-input').value = '';
        document.getElementById('icon-input').value = '';
        document.getElementById('private-checkbox').checked = false;
        hideAddDialog();

        logAction('添加卡片', { name, url, tips, icon, category, isPrivate });
    }

    // 删除卡片
    async function removeCard(card) {
        if (!await validateToken()) {
            return;
        }
        const name = card.querySelector('.card-title').textContent;
        const url = card.getAttribute('data-url');
        const isPrivate = card.dataset.isPrivate === 'true';

        const confirmed = await customConfirm('确定要删除 "' + name + '" 吗？', '确定', '取消');
        if (!confirmed) {
            return;
        }

        links = links.filter(link => link.url !== url);
        if (isPrivate) {
            privateLinks = privateLinks.filter(link => link.url !== url);
        } else {
            publicLinks = publicLinks.filter(link => link.url !== url);
        }

        for (const key in categories) {
            categories[key] = categories[key].filter(link => link.url !== url);
        }

        card.remove();

        saveLinks();

        logAction('删除卡片', { name, url, isPrivate });
    }

    // 拖拽卡片
    let draggedCard = null;
    let touchStartX, touchStartY;

    // 触屏端拖拽卡片
    function touchStart(event) {
        if (!isAdmin) {
            return;
        }
        draggedCard = event.target.closest('.card');
        if (!draggedCard) return;

        event.preventDefault();
        const touch = event.touches[0];
        touchStartX = touch.clientX;
        touchStartY = touch.clientY;

        draggedCard.classList.add('dragging');

        document.addEventListener('touchmove', touchMove, { passive: false });
        document.addEventListener('touchend', touchEnd);

    }

    function touchMove(event) {
        if (!draggedCard) return;
        event.preventDefault();

        const touch = event.touches[0];
        const currentX = touch.clientX;
        const currentY = touch.clientY;

        const deltaX = currentX - touchStartX;
        const deltaY = currentY - touchStartY;
        draggedCard.style.transform = "translate(" + deltaX + "px, " + deltaY + "px)";

        const target = findCardUnderTouch(currentX, currentY);
        if (target && target !== draggedCard) {
            const container = target.parentElement;
            const targetRect = target.getBoundingClientRect();

            if (currentX < targetRect.left + targetRect.width / 2) {
                container.insertBefore(draggedCard, target);
            } else {
                container.insertBefore(draggedCard, target.nextSibling);
            }
        }
    }

    function touchEnd(event) {
        if (!draggedCard) return;

        const card = draggedCard;
        const targetCategory = card.closest('.card-container').id;

        // 🔧 优化：删除冗余验证，拖拽只在管理员模式下可用，saveCardOrder()内部已有验证
        if (isAdmin && card) {
            updateCardCategory(card, targetCategory);
            saveCardOrder().catch(error => {
                console.error('Save failed:', error);
            });
        }
        cleanupDragState();
    }

    function findCardUnderTouch(x, y) {
        const cards = document.querySelectorAll('.card:not(.dragging)');
        return Array.from(cards).find(card => {
            const rect = card.getBoundingClientRect();
            return x >= rect.left && x <= rect.right && y >= rect.top && y <= rect.bottom;
        });
    }

    // PC端拖拽卡片
    function dragStart(event) {
        if (!isAdmin) {
            event.preventDefault();
            return;
        }
        draggedCard = event.target.closest('.card');
        if (!draggedCard) return;

        draggedCard.classList.add('dragging');
        event.dataTransfer.effectAllowed = "move";
        logAction('开始拖拽卡片', { name: draggedCard.querySelector('.card-title').textContent });
    }

    function dragOver(event) {
        if (!isAdmin) {
            event.preventDefault();
            return;
        }
        event.preventDefault();
        const target = event.target.closest('.card');
        if (target && target !== draggedCard) {
            const container = target.parentElement;
            const mousePositionX = event.clientX;
            const targetRect = target.getBoundingClientRect();

            if (mousePositionX < targetRect.left + targetRect.width / 2) {
                container.insertBefore(draggedCard, target);
            } else {
                container.insertBefore(draggedCard, target.nextSibling);
            }
        }
    }

    // 清理拖拽状态函数
    function cleanupDragState() {
        if (draggedCard) {
            draggedCard.classList.remove('dragging');
            draggedCard.style.transform = '';
            draggedCard = null;
        }

        document.removeEventListener('touchmove', touchMove);
        document.removeEventListener('touchend', touchEnd);

        touchStartX = null;
        touchStartY = null;
    }

    // PC端拖拽结束
    function drop(event) {
        if (!isAdmin) {
            event.preventDefault();
            return;
        }
        event.preventDefault();

        const card = draggedCard;
        const targetCategory = event.target.closest('.card-container').id;

        validateToken().then(isValid => {
            if (isValid && card) {
                updateCardCategory(card, targetCategory);
                saveCardOrder().catch(error => {
                    console.error('Save failed:', error);
                });
            }
            cleanupDragState();
        });
    }

    function dragEnd(event) {
        if (draggedCard) {
            draggedCard.classList.remove('dragging');
            logAction('拖拽卡片结束');
        }
    }

    // 更新卡片分类
    function updateCardCategory(card, newCategory) {
        const cardTitle = card.querySelector('.card-title').textContent;
        const cardUrl = card.getAttribute('data-url');
        const isPrivate = card.dataset.isPrivate === 'true';

        const linkIndex = links.findIndex(link => link.url === cardUrl);
        if (linkIndex !== -1) {
            links[linkIndex].category = newCategory;
        }

        const linkArray = isPrivate ? privateLinks : publicLinks;
        const arrayIndex = linkArray.findIndex(link => link.url === cardUrl);
        if (arrayIndex !== -1) {
            linkArray[arrayIndex].category = newCategory;
        }

        card.dataset.category = newCategory;
    }

    // 在页面加载完成后添加触摸事件监听器
    document.addEventListener('DOMContentLoaded', function() {
        const cardContainers = document.querySelectorAll('.card-container');
        cardContainers.forEach(container => {
            container.addEventListener('touchstart', touchStart, { passive: false });
        });
    });

    // 保存卡片顺序
    async function saveCardOrder() {
        if (!await validateToken()) {
            return;
        }
        const containers = document.querySelectorAll('.card-container');
        let newPublicLinks = [];
        let newPrivateLinks = [];
        let newCategories = {};

        containers.forEach(container => {
            const category = container.id;
            newCategories[category] = [];

            [...container.children].forEach(card => {
                const url = card.getAttribute('data-url');
                const name = card.querySelector('.card-title').textContent;
                const isPrivate = card.dataset.isPrivate === 'true';
                card.dataset.category = category;

                // 从原始链接数据中获取描述和图标信息
                const originalLink = links.find(link => link.url === url);
                const tips = originalLink?.tips || '';
                const icon = originalLink?.icon || '';

                const link = { name, url, tips, icon, category, isPrivate };
                if (isPrivate) {
                    newPrivateLinks.push(link);
                } else {
                    newPublicLinks.push(link);
                }
                newCategories[category].push(link);
            });
        });

        publicLinks.length = 0;
        publicLinks.push(...newPublicLinks);
        privateLinks.length = 0;
        privateLinks.push(...newPrivateLinks);
        Object.keys(categories).forEach(key => delete categories[key]);
        Object.assign(categories, newCategories);

        try {
            const response = await fetch('/api/saveOrder', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': localStorage.getItem('authToken')
                },
                body: JSON.stringify({
                    userId: 'testUser',
                    links: [...newPublicLinks, ...newPrivateLinks],
                    categories: newCategories
                }),
            });
            const result = await response.json();
            if (!result.success) {
                throw new Error('Failed to save order');
            }
            logAction('保存卡片顺序', { publicCount: newPublicLinks.length, privateCount: newPrivateLinks.length, categoryCount: Object.keys(newCategories).length });
        } catch (error) {
            logAction('保存顺序失败', { error: error.message });
            await customAlert('保存顺序失败，请重试', '保存失败');
        }
    }

    // 设置状态重新加载卡片
    async function reloadCardsAsAdmin() {
        document.querySelectorAll('.card-container').forEach(container => {
            container.innerHTML = '';
        });
        await loadLinks();
        logAction('重新加载卡片（管理员模式）');
    }

    // 处理登录按钮点击
    async function handleLoginClick() {
        if (isLoggedIn) {
            // 如果已登录，退出登录
            const confirmed = await customConfirm('确定要退出登录吗？', '确定', '取消');
            if (confirmed) {
                await logout();
            }
        } else {
            // 如果未登录，显示登录弹窗
            showLoginModal();
        }
    }

    // 显示登录弹窗
    function showLoginModal() {
        document.getElementById('login-modal').style.display = 'flex';
        document.getElementById('login-password').focus();
    }

    // 隐藏登录弹窗
    function hideLoginModal() {
        document.getElementById('login-modal').style.display = 'none';
        document.getElementById('login-password').value = '';
    }

    // 执行登录
    async function performLogin() {
        const password = document.getElementById('login-password').value;
        if (!password) {
            await customAlert('请输入密码', '提示');
            return;
        }

        try {
            const result = await verifyPassword(password);
            if (result.valid) {
                isLoggedIn = true;
                localStorage.setItem('authToken', result.token);
                console.log('Token saved:', result.token);
                loadLinks();
                hideLoginModal();
                updateLoginButton();
                await customAlert('登录成功！', '登录');
                logAction('登录成功');
            } else {
                await customAlert('密码错误', '登录失败');
                logAction('登录失败', { reason: result.error || '密码错误' });
            }
        } catch (error) {
            // 🔧 安全修复：避免泄露详细错误信息
            console.error('Login error occurred');
            await customAlert('登录过程出错，请重试', '错误');
        }
    }

    // 退出登录
    async function logout() {
        isLoggedIn = false;
        isAdmin = false;
        localStorage.removeItem('authToken');
        links = publicLinks;
        renderSections();
        updateLoginButton();
        await customAlert('退出登录成功！', '退出登录');
        updateUIState();
        logAction('退出登录');
    }

    // 更新按钮状态
    function updateLoginButton() {
        const loginBtn = document.getElementById('login-btn');
        const adminBtn = document.getElementById('admin-btn');

        if (isLoggedIn) {
            loginBtn.textContent = '退出登录';
            adminBtn.style.display = 'inline-block';
            if (isAdmin) {
                adminBtn.textContent = '离开设置';
            } else {
                adminBtn.textContent = '设置';
            }
        } else {
            loginBtn.textContent = '登录';
            adminBtn.style.display = 'none';
        }
    }

    // 打开GitHub仓库
    function openGitHub() {
        window.open('https://github.com/omaler886/Card-Tab', '_blank');
        logAction('访问GitHub仓库');
    }

    // 切换书签搜索下拉框
    function toggleBookmarkSearch() {
        const dropdown = document.getElementById('bookmark-search-dropdown');
        const isVisible = dropdown.classList.contains('show');

        if (isVisible) {
            dropdown.classList.remove('show');
        } else {
            dropdown.classList.add('show');
            document.getElementById('bookmark-search-input').focus();
        }
    }

    // 点击页面其他地方关闭书签搜索下拉框
    document.addEventListener('click', function(event) {
        const searchToggle = document.querySelector('.bookmark-search-toggle');
        const dropdown = document.getElementById('bookmark-search-dropdown');

        if (!searchToggle.contains(event.target)) {
            dropdown.classList.remove('show');
        }
    });

    // 登录密码输入框回车事件
    document.getElementById('login-password').addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            performLogin();
        }
    });

    // 切换设置状态
    async function toggleAdminMode() {
        const addRemoveControls = document.querySelector('.add-remove-controls');

        if (!isAdmin && isLoggedIn) {
            if (!await validateToken()) {
                return;
            }

            // 显示加载状态
            showLoading('正在进入设置模式...');

            // 在进入设置模式之前进行备份
            try {
                const response = await fetch('/api/backupData', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': localStorage.getItem('authToken')
                    },
                    body: JSON.stringify({
                        sourceUserId: 'testUser',
                        backupUserId: 'backup'
                    }),
                });
                const result = await response.json();
                if (result.success) {
                    logAction('数据备份成功');
                } else {
                    throw new Error('备份失败');
                }
            } catch (error) {
                // 🔧 安全修复：避免泄露详细错误信息
                logAction('数据备份失败', { error: 'Backup operation failed' });
                hideLoading();
                const confirmed = await customConfirm('备份失败，是否仍要继续进入设置模式？', '是', '否');
                if (!confirmed) {
                    return;
                }
                showLoading('正在进入设置模式...');
            }

            try {
                isAdmin = true;
                addRemoveControls.style.display = 'flex';
                await reloadCardsAsAdmin();
                logAction('进入设置');
                hideLoading();
                await customAlert('准备设置分类和书签', '设置模式');
            } finally {
                hideLoading();
            }
        } else if (isAdmin) {
            isAdmin = false;
            removeMode = false;
            isRemoveCategoryMode = false;
            isEditCategoryMode = false;

            // 重置分类管理按钮状态
            const manageButton = document.querySelector('.category-manage-btn');
            if (manageButton) {
                manageButton.classList.remove('active');
            }

            addRemoveControls.style.display = 'none';
            await reloadCardsAsAdmin();
            logAction('离开设置');
            await customAlert('设置已保存', '设置完成');
        }

        updateLoginButton();
        updateUIState();
    }



    function getSavedThemeMode() {
        const mode = localStorage.getItem(THEME_MODE_KEY);
        return ['auto', 'light', 'dark'].includes(mode) ? mode : 'auto';
    }

    function getThemeMediaQuery() {
        if (!themeMediaQuery && typeof window.matchMedia === 'function') {
            themeMediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
        }
        return themeMediaQuery;
    }

    function getAutoThemeState() {
        const hour = new Date().getHours();
        const isNight = hour >= 18 || hour < 6;
        const mediaQuery = getThemeMediaQuery();
        const prefersDark = mediaQuery ? mediaQuery.matches : false;

        if (prefersDark && isNight) {
            return { isDark: true, reason: '系统深色 + 夜间' };
        }
        if (prefersDark) {
            return { isDark: true, reason: '系统深色' };
        }
        if (isNight) {
            return { isDark: true, reason: '夜间自动切换' };
        }
        return { isDark: false, reason: '白天自动切换' };
    }

    function applyThemeAppearance(shouldUseDark) {
        if (shouldUseDark) {
            document.body.classList.add('dark-theme');
        } else {
            document.body.classList.remove('dark-theme');
        }
        isDarkTheme = shouldUseDark;
    }

    function updateThemeToggleUI(reason) {
        const themeToggleBtn = document.getElementById('theme-toggle');
        if (!themeToggleBtn) return;

        const modeConfig = {
            auto: { label: '自', title: '主题模式：自动' },
            light: { label: '白', title: '主题模式：浅色' },
            dark: { label: '黑', title: '主题模式：深色' }
        };
        const currentConfig = modeConfig[currentThemeMode] || modeConfig.auto;
        themeToggleBtn.textContent = currentConfig.label;
        themeToggleBtn.dataset.themeMode = currentThemeMode;
        themeToggleBtn.title = reason ? currentConfig.title + '（' + reason + '）' : currentConfig.title;
    }

    function applyThemeMode(mode, options) {
        const config = options || {};
        currentThemeMode = mode;
        const resolvedTheme = mode === 'auto'
            ? getAutoThemeState()
            : { isDark: mode === 'dark', reason: mode === 'dark' ? '手动深色' : '手动浅色' };

        applyThemeAppearance(resolvedTheme.isDark);
        updateThemeToggleUI(resolvedTheme.reason);

        if (config.log !== false) {
            logAction('应用主题模式', {
                mode: currentThemeMode,
                isDarkTheme,
                reason: resolvedTheme.reason
            });
        }
    }

    function setThemeMode(mode, options) {
        const config = options || {};
        currentThemeMode = ['auto', 'light', 'dark'].includes(mode) ? mode : 'auto';

        if (config.persist !== false) {
            localStorage.setItem(THEME_MODE_KEY, currentThemeMode);
        }

        applyThemeMode(currentThemeMode, { log: config.log });
    }

    function applyDarkTheme() {
        applyThemeAppearance(true);
        updateThemeToggleUI('手动深色');
        logAction('应用暗色主题');
    }

    function syncThemeFromEnvironment() {
        if (currentThemeMode === 'auto') {
            applyThemeMode('auto', { log: false });
        }
    }

    function initThemeSystem() {
        currentThemeMode = getSavedThemeMode();
        applyThemeMode(currentThemeMode, { log: false });

        const mediaQuery = getThemeMediaQuery();
        if (mediaQuery && !themeMediaListenerBound) {
            const syncHandler = function() {
                syncThemeFromEnvironment();
            };
            if (typeof mediaQuery.addEventListener === 'function') {
                mediaQuery.addEventListener('change', syncHandler);
            } else if (typeof mediaQuery.addListener === 'function') {
                mediaQuery.addListener(syncHandler);
            }
            themeMediaListenerBound = true;
        }

        if (!themeVisibilityListenerBound) {
            document.addEventListener('visibilitychange', function() {
                if (!document.hidden) {
                    syncThemeFromEnvironment();
                }
            });
            themeVisibilityListenerBound = true;
        }

        if (!themeAutoRefreshTimer) {
            themeAutoRefreshTimer = setInterval(function() {
                syncThemeFromEnvironment();
            }, 60000);
        }
    }

    // 全局变量用于管理对话框事件处理器
    let currentConfirmHandler = null;
    let currentCancelHandler = null;

    // 显示编辑链接对话框
    function showEditDialog(link) {
        document.getElementById('dialog-overlay').style.display = 'flex';

        document.getElementById('name-input').value = link.name;
        document.getElementById('url-input').value = link.url;
        document.getElementById('tips-input').value = link.tips || '';
        document.getElementById('icon-input').value = link.icon || '';
        document.getElementById('category-select').value = link.category;
        document.getElementById('private-checkbox').checked = link.isPrivate;

        const confirmBtn = document.getElementById('dialog-confirm-btn');
        const cancelBtn = document.getElementById('dialog-cancel-btn');

        // 清除所有旧的事件处理器
        confirmBtn.onclick = null;
        cancelBtn.onclick = null;
        if (currentConfirmHandler) {
            confirmBtn.removeEventListener('click', currentConfirmHandler);
        }
        if (currentCancelHandler) {
            cancelBtn.removeEventListener('click', currentCancelHandler);
        }

        // 设置新的事件处理器
        currentConfirmHandler = async function (event) {
            event.preventDefault();
            event.stopPropagation();
            await updateLink(link);
        };

        currentCancelHandler = function(event) {
            event.preventDefault();
            event.stopPropagation();
            hideAddDialog();
        };

        confirmBtn.addEventListener('click', currentConfirmHandler);
        cancelBtn.addEventListener('click', currentCancelHandler);

        logAction('显示编辑链接对话框');
    }

    // 显示添加链接对话框
    function showAddDialog() {
        document.getElementById('dialog-overlay').style.display = 'flex';

        const nameInput = document.getElementById('name-input');
        nameInput.value = '';
        document.getElementById('url-input').value = '';
        document.getElementById('tips-input').value = '';
        document.getElementById('icon-input').value = '';
        document.getElementById('private-checkbox').checked = false;

        const confirmBtn = document.getElementById('dialog-confirm-btn');
        const cancelBtn = document.getElementById('dialog-cancel-btn');

        // 清除所有旧的事件处理器
        confirmBtn.onclick = null;
        cancelBtn.onclick = null;
        if (currentConfirmHandler) {
            confirmBtn.removeEventListener('click', currentConfirmHandler);
        }
        if (currentCancelHandler) {
            cancelBtn.removeEventListener('click', currentCancelHandler);
        }

        // 设置新的事件处理器
        currentConfirmHandler = async function (event) {
            event.preventDefault();
            event.stopPropagation();
            await addLink();
        };

        currentCancelHandler = function(event) {
            event.preventDefault();
            event.stopPropagation();
            hideAddDialog();
        };

        confirmBtn.addEventListener('click', currentConfirmHandler);
        cancelBtn.addEventListener('click', currentCancelHandler);

        setTimeout(() => {
            nameInput.focus();
        }, 50);

        logAction('显示添加链接对话框');
    }

    // 更新链接
    async function updateLink(oldLink) {
        if (!await validateToken()) return;

        const name = document.getElementById('name-input').value.trim();
        const url = document.getElementById('url-input').value.trim();
        const tips = document.getElementById('tips-input').value.trim();
        const icon = document.getElementById('icon-input').value.trim();
        const category = document.getElementById('category-select').value;
        const isPrivate = document.getElementById('private-checkbox').checked;

        // 验证必填字段
        if (!name || !url || !category) {
            let errorMessage = '';
            if (!name && !url) {
                errorMessage = '请输入名称和URL';
            } else if (!name) {
                errorMessage = '请输入名称';
            } else if (!url) {
                errorMessage = '请输入URL';
            }

            await customAlert(errorMessage, '编辑卡片');
            if (!name) {
                document.getElementById('name-input').focus();
            } else if (!url) {
                document.getElementById('url-input').focus();
            }
            return;
        }

        // 规范化 URL 并检查是否与其他链接重复（排除当前编辑的链接）
        const normalizedUrl = normalizeUrl(url);
        const normalizedOldUrl = normalizeUrl(oldLink.url);
        const allLinks = [...publicLinks, ...privateLinks];

        // 预处理：一次性规范化所有 URL（性能优化）
        const normalizedUrls = allLinks.map(link => normalizeUrl(link.url));
        const isUrlExists = normalizedUrls.some(nUrl =>
            nUrl === normalizedUrl && nUrl !== normalizedOldUrl
        );

        if (isUrlExists) {
            await customAlert('该URL已存在，请勿重复添加', '编辑卡片');
            document.getElementById('url-input').focus();
            return;
        }

        // 存储规范化后的 URL
        const updatedLink = { name, url: normalizedUrl, tips, icon, category, isPrivate };

        try {
            // 替换旧链接
            const list = oldLink.isPrivate ? privateLinks : publicLinks;
            const listNormalizedUrls = list.map(l => normalizeUrl(l.url));
            const index = listNormalizedUrls.indexOf(normalizedOldUrl);
            if (index !== -1) {
                list[index] = updatedLink;
            }

            // 同步更新 links
            links = isLoggedIn ? [...publicLinks, ...privateLinks] : publicLinks;

            await saveLinks();
            renderSections();
            hideAddDialog();

            logAction('更新卡片', { oldUrl: oldLink.url, name, url, tips, icon, category, isPrivate });
        } catch (error) {
            logAction('更新卡片失败:', error);
            await customAlert('更新卡片失败:' + error.message, '编辑卡片');
        }
    }

    // 隐藏添加链接对话框
    function hideAddDialog() {
        document.getElementById('dialog-overlay').style.display = 'none';

        // 清理事件处理器
        const confirmBtn = document.getElementById('dialog-confirm-btn');
        const cancelBtn = document.getElementById('dialog-cancel-btn');

        if (currentConfirmHandler) {
            confirmBtn.removeEventListener('click', currentConfirmHandler);
            currentConfirmHandler = null;
        }
        if (currentCancelHandler) {
            cancelBtn.removeEventListener('click', currentCancelHandler);
            currentCancelHandler = null;
        }

        confirmBtn.onclick = null;
        cancelBtn.onclick = null;

        logAction('隐藏添加链接对话框');
    }

    // 切换编辑卡片模式
    function toggleRemoveMode() {
        removeMode = !removeMode;
        const editButtons = document.querySelectorAll('.edit-btn');
        const deleteButtons = document.querySelectorAll('.delete-btn');

        editButtons.forEach(btn => {
            btn.style.display = removeMode ? 'flex' : 'none';
        });
        deleteButtons.forEach(btn => {
            btn.style.display = removeMode ? 'flex' : 'none';
        });

        // 隐藏自定义提示框
        document.getElementById('custom-tooltip').style.display = 'none';

        // 切换卡片悬停效果
        const cards = document.querySelectorAll('.card');
        cards.forEach(card => {
            if (removeMode) {
                card.classList.add('no-hover');
            } else {
                card.classList.remove('no-hover');
            }
        });

        logAction('切换编辑卡片模式', { removeMode });
    }



    // 切换主题
    function toggleTheme() {
        const modeSequence = ['auto', 'light', 'dark'];
        const currentIndex = modeSequence.indexOf(currentThemeMode);
        const nextMode = modeSequence[(currentIndex + 1) % modeSequence.length];
        setThemeMode(nextMode, { log: true });
    }

    initThemeSystem();

    // 返回顶部
    function scrollToTop() {
        window.scrollTo({
            top: 0,
            behavior: 'smooth'
        });
        logAction('返回顶部');
    }

    // 控制返回顶部按钮显示/隐藏
    function handleBackToTopVisibility() {
        const btn = document.getElementById('back-to-top-btn');
        if (!btn) return;

        // 如果页面滚动高度大于 300px，才显示按钮
        if (window.scrollY > 300) {
            btn.style.display = 'flex';
        } else {
            btn.style.display = 'none';
        }
    }

    // 处理鼠标悬停提示
    function handleTooltipMouseMove(e, tips, isAdmin) {
        const tooltip = document.getElementById('custom-tooltip');

        if (!tips || isAdmin) {
            tooltip.style.display = 'none';
            return;
        }

        // 设置提示内容
        if (tooltip.textContent !== tips) {
            tooltip.textContent = tips;
        }

        tooltip.style.display = 'block';

        const offsetX = 15;
        const offsetY = 10;

        const tooltipRect = tooltip.getBoundingClientRect();
        const pageWidth = window.innerWidth;
        const pageHeight = window.innerHeight;

        let left = e.pageX + offsetX;
        let top = e.pageY + offsetY;

        if (pageWidth - e.clientX < 200) {
            left = e.pageX - tooltipRect.width - offsetX;
        }
        // 如果距离底部小于100像素，往上显示
        if (pageHeight - e.clientY < 100) {
            top = e.pageY - tooltipRect.height - offsetY;
        }

        tooltip.style.left = left + 'px';
        tooltip.style.top = top + 'px';
    }

    function handleTooltipMouseLeave() {
        const tooltip = document.getElementById('custom-tooltip');
        tooltip.style.display = 'none';
    }

    // 验证密码
    async function verifyPassword(inputPassword) {
        const response = await fetch('/api/verifyPassword', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ password: inputPassword }),
        });
        const result = await response.json();
        return result;
    }

    // 全局变量，标记是否正在显示搜索结果
    let isShowingSearchResults = false;

    // 书签搜索功能 - 简化版
    function searchBookmarks(query) {
        if (!query || query.trim() === '') {
            hideSearchResults();
            return;
        }

        query = query.toLowerCase().trim();
        const sectionsContainer = document.getElementById('sections-container');

        // 只搜索书签名称，简化搜索逻辑
        const visibleLinks = links;
        const matchedLinks = visibleLinks.filter(link =>
            link.name.toLowerCase().includes(query)
        );

        // 清空主内容区域
        sectionsContainer.innerHTML = '';

        // 创建搜索结果头部
        const searchHeader = document.createElement('div');
        searchHeader.className = 'search-results-header';

        const searchTitle = document.createElement('div');
        searchTitle.className = 'search-results-title';
        searchTitle.textContent = '搜索结果 (' + matchedLinks.length + '个)';

        const backButton = document.createElement('button');
        backButton.className = 'back-to-main';
        backButton.textContent = '返回主页';
        backButton.onclick = hideSearchResults;

        searchHeader.appendChild(searchTitle);
        searchHeader.appendChild(backButton);
        sectionsContainer.appendChild(searchHeader);

        if (matchedLinks.length === 0) {
            const noResults = document.createElement('div');
            noResults.className = 'no-search-results';
            noResults.textContent = '没有找到匹配的书签';
            noResults.style.textAlign = 'center';
            noResults.style.padding = '40px';
            noResults.style.color = '#666';
            sectionsContainer.appendChild(noResults);
        } else {
            // 创建简单的搜索结果容器
            const resultsSection = document.createElement('div');
            resultsSection.className = 'search-results-section';

            const cardContainer = document.createElement('div');
            cardContainer.className = 'card-container';

            // 为每个匹配的链接创建卡片
            matchedLinks.forEach(link => {
                createCard(link, cardContainer);
            });

            resultsSection.appendChild(cardContainer);
            sectionsContainer.appendChild(resultsSection);
        }

        // 设置搜索状态标记
        isShowingSearchResults = true;

        // 隐藏分类按钮
        const categoryButtonsContainer = document.getElementById('category-buttons-container');
        if (categoryButtonsContainer) {
            categoryButtonsContainer.style.display = 'none';
        }

        logAction('执行书签搜索', { query, resultCount: matchedLinks.length });
    }

    // 隐藏搜索结果 - 简化版
    function hideSearchResults() {
        // 重置标记
        isShowingSearchResults = false;

        // 清空搜索框
        document.getElementById('bookmark-search-input').value = '';

        // 重新渲染正常的分类和书签
        renderSections();

        // 显示分类按钮
        const categoryButtonsContainer = document.getElementById('category-buttons-container');
        if (categoryButtonsContainer) {
            categoryButtonsContainer.style.display = 'flex';
        }

        // 重新渲染分类按钮，确保分类按钮的正确显示
        renderCategoryButtons();
    }

    // 书签搜索输入框回车事件
    document.getElementById('bookmark-search-input').addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            const query = document.getElementById('bookmark-search-input').value;
            searchBookmarks(query);
            // 搜索后关闭下拉框
            document.getElementById('bookmark-search-dropdown').classList.remove('show');
        }
    });

    // 书签搜索输入框实时搜索
    document.getElementById('bookmark-search-input').addEventListener('input', (e) => {
        const query = e.target.value;
        if (query.trim() === '') {
            hideSearchResults();
        } else {
            searchBookmarks(query);
        }
    });



    // 初始化加载
    async function bootMainApp() {
        if (mainAppBooted) return;
        mainAppBooted = true;
        try {
            await validateToken();
            updateLoginButton();
            await loadLinks();
            // 初始加载完成后，检测当前可见分类
            setTimeout(setActiveCategoryButtonByVisibility, 500);
            // 初始化返回顶部按钮状态
            setTimeout(handleBackToTopVisibility, 100);
        } catch (error) {
            // 🔧 安全修复：避免泄露详细错误信息
            console.error('Initialization failed');
            setBootStatus('页面初始化失败，请检查浏览器控制台或 Worker 日志。', true);
        }
    }

    // 添加滚动事件监听器
    window.addEventListener('scroll', handleBackToTopVisibility);


    // 前端检查是否有 token
    async function validateToken() {
        const token = localStorage.getItem('authToken');
        if (!token) {
            isLoggedIn = false;
            updateUIState();
            return false;
        }

        try {
            const response = await fetchWithTimeout('/api/getLinks?userId=testUser', {
                headers: { 'Authorization': token }
            }, 8000);

            if (response.status === 401) {
                await resetToLoginState('token已过期，请重新登录');
                return false;
            }

            isLoggedIn = true;
            updateUIState();
            return true;
        } catch (error) {
            // 🔧 安全修复：避免泄露详细错误信息
            console.error('Token validation failed');
            return false;
        }
    }

    // 重置状态
    async function resetToLoginState(message) {
        // 🔧 修复：显示用户可见的Token过期提示
        if (message && message.trim() !== '') {
            await customAlert(message, '登录状态');
        }

        cleanupDragState();

        localStorage.removeItem('authToken');
        isLoggedIn = false;
        isAdmin = false;
        removeMode = false;
        isRemoveCategoryMode = false;
        isEditCategoryMode = false;

        updateLoginButton();
        updateUIState();
        links = publicLinks;
        renderSections();

        const addRemoveControls = document.querySelector('.add-remove-controls');
        if (addRemoveControls) {
            addRemoveControls.style.display = 'none';
        }

        document.querySelectorAll('.delete-btn').forEach(btn => {
            btn.style.display = 'none';
        });

        document.querySelectorAll('.delete-category-btn').forEach(btn => {
            btn.style.display = 'none';
        });

        document.querySelectorAll('.edit-category-btn').forEach(btn => {
            btn.style.display = 'none';
        });

        document.querySelectorAll('.move-category-btn').forEach(btn => {
            btn.style.display = 'none';
        });

        // 重置分类管理按钮状态
        const manageButton = document.querySelector('.category-manage-btn');
        if (manageButton) {
            manageButton.classList.remove('active');
        }

        const dialogOverlay = document.getElementById('dialog-overlay');
        if (dialogOverlay) {
            dialogOverlay.style.display = 'none';
        }

        const loginModal = document.getElementById('login-modal');
        if (loginModal) {
            loginModal.style.display = 'none';
        }

        // 确保按钮状态正确重置
        const adminBtn = document.getElementById('admin-btn');
        if (adminBtn) {
            adminBtn.style.display = 'none';
        }
    }

    // 自定义Alert对话框
    function customAlert(message, title = '提示', confirmText = '确定') {
        return new Promise((resolve) => {
            const overlay = document.getElementById('custom-alert-overlay');
            const titleEl = document.getElementById('custom-alert-title');
            const contentEl = document.getElementById('custom-alert-content');
            const confirmBtn = document.getElementById('custom-alert-confirm');

            // 设置内容
            titleEl.textContent = title;
            contentEl.textContent = message;
            confirmBtn.textContent = confirmText;

            // 显示弹窗
            overlay.style.display = 'flex';

            // 确认按钮事件
            const handleConfirm = () => {
                overlay.style.display = 'none';
                confirmBtn.removeEventListener('click', handleConfirm);
                document.removeEventListener('keydown', handleKeyDown);
                resolve();
            };

            confirmBtn.addEventListener('click', handleConfirm);

            // ESC键关闭
            const handleKeyDown = (e) => {
                if (e.key === 'Escape') {
                    handleConfirm();
                }
            };

            document.addEventListener('keydown', handleKeyDown);

            // 点击遮罩层关闭
            overlay.addEventListener('click', (e) => {
                if (e.target === overlay) {
                    handleConfirm();
                }
            });
        });
    }

    // 自定义Confirm对话框
    function customConfirm(message, okText = '确定', cancelText = '取消') {
        return new Promise((resolve) => {
            const overlay = document.getElementById('custom-confirm-overlay');
            const messageEl = document.getElementById('custom-confirm-message');
            const okBtn = document.getElementById('custom-confirm-ok');
            const cancelBtn = document.getElementById('custom-confirm-cancel');

            // 设置弹窗内容
            messageEl.textContent = message;
            okBtn.textContent = okText;
            cancelBtn.textContent = cancelText;

            // 显示弹窗
            overlay.style.display = 'flex';

            // 事件处理函数
            const handleConfirm = (result) => {
                cleanup();
                resolve(result);
            };

            const handleKeyDown = (e) => {
                if (e.key === 'Enter') handleConfirm(true);
                if (e.key === 'Escape') handleConfirm(false);
            };

            // 清理函数
            const cleanup = () => {
                overlay.style.display = 'none';
                document.removeEventListener('keydown', handleKeyDown);
                okBtn.onclick = null;
                cancelBtn.onclick = null;
                overlay.onclick = null;
            };

            // 绑定事件
            okBtn.onclick = () => handleConfirm(true);
            cancelBtn.onclick = () => handleConfirm(false);
            document.addEventListener('keydown', handleKeyDown);
            overlay.onclick = (e) => e.target === overlay && handleConfirm(false);
        });
    }

    // 分类名称输入对话框
    function showCategoryDialog(title, defaultValue = '') {
        return new Promise((resolve) => {
            const dialog = document.getElementById('category-dialog');
            const input = document.getElementById('category-name-input');
            const titleEl = document.getElementById('category-dialog-title');
            const confirmBtn = document.getElementById('category-confirm-btn');
            const cancelBtn = document.getElementById('category-cancel-btn');

            // 设置弹窗内容
            titleEl.textContent = title;
            input.value = defaultValue;

            // 显示弹窗
            dialog.style.display = 'flex';
            setTimeout(() => input.focus(), 50);

            // 事件处理函数
            const handleConfirm = () => {
                const value = input.value.trim();
                if (value) {
                    cleanup();
                    resolve(value);
                } else {
                    input.focus();
                }
            };

            const handleCancel = () => {
                cleanup();
                resolve(null);
            };

            const handleKeyDown = (e) => {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    handleConfirm();
                } else if (e.key === 'Escape') {
                    handleCancel();
                }
            };

            // 清理函数
            const cleanup = () => {
                dialog.style.display = 'none';
                document.removeEventListener('keydown', handleKeyDown);
                confirmBtn.onclick = null;
                cancelBtn.onclick = null;
                dialog.onclick = null;
            };

            // 绑定事件
            confirmBtn.onclick = handleConfirm;
            cancelBtn.onclick = handleCancel;
            document.addEventListener('keydown', handleKeyDown);
            dialog.onclick = (e) => e.target === dialog && handleCancel();
        });
    }

    // 显示加载遮罩
    function showLoading(message = '加载中，请稍候...') {
        const mask = document.getElementById('loading-mask');
        const textElement = mask.querySelector('p');
        textElement.textContent = message;
        mask.style.display = 'flex';
    }

    // 隐藏加载遮罩
    function hideLoading() {
        const mask = document.getElementById('loading-mask');
        mask.style.display = 'none';
    }

    // ========== 天气组件 ==========
    const WEATHER_API = '/api/weather';  // 后端代理
    const WEATHER_CACHE_KEY = 'card_tab_weather_cache';
    const WEATHER_CACHE_DURATION = 30 * 60 * 1000; // 30分钟
    let weatherNotConfigured = false; // 天气服务是否未配置

    // 天气图标映射
    const WEATHER_ICONS = {
        '100': '☀️', '150': '🌙', '101': '⛅', '102': '⛅', '103': '🌥️', '104': '☁️',
        '151': '🌙', '152': '🌙', '153': '🌙', '154': '☁️',
        '300': '🌦️', '301': '🌧️', '302': '⛈️', '303': '⛈️', '304': '⛈️',
        '305': '🌧️', '306': '🌧️', '307': '🌧️', '308': '🌧️', '309': '🌧️',
        '310': '🌧️', '311': '🌧️', '312': '🌧️', '313': '🌧️',
        '400': '🌨️', '401': '🌨️', '402': '🌨️', '403': '🌨️', '404': '🌨️',
        '500': '🌫️', '501': '🌫️', '502': '🌫️', '503': '🌫️', '504': '🌫️',
        '999': '❓'
    };
    const WEEKDAYS = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
    const EDGE_TIMEZONE_CITY_MAP = {
        'Asia/Shanghai': { query: '上海', label: '上海' },
        'Asia/Hong_Kong': { query: '香港', label: '香港' },
        'Asia/Tokyo': { query: '东京', label: '东京' },
        'Asia/Seoul': { query: '首尔', label: '首尔' },
        'Asia/Singapore': { query: '新加坡', label: '新加坡' },
        'Asia/Bangkok': { query: '曼谷', label: '曼谷' },
        'Asia/Kuala_Lumpur': { query: '吉隆坡', label: '吉隆坡' },
        'Asia/Jakarta': { query: '雅加达', label: '雅加达' },
        'Asia/Kolkata': { query: '孟买', label: '孟买' },
        'Asia/Dubai': { query: '迪拜', label: '迪拜' },
        'Europe/London': { query: '伦敦', label: '伦敦' },
        'Europe/Paris': { query: '巴黎', label: '巴黎' },
        'Europe/Berlin': { query: '柏林', label: '柏林' },
        'Europe/Madrid': { query: '马德里', label: '马德里' },
        'Europe/Moscow': { query: '莫斯科', label: '莫斯科' },
        'America/New_York': { query: '纽约', label: '纽约' },
        'America/Chicago': { query: '芝加哥', label: '芝加哥' },
        'America/Denver': { query: '丹佛', label: '丹佛' },
        'America/Los_Angeles': { query: '洛杉矶', label: '洛杉矶' },
        'America/Phoenix': { query: '菲尼克斯', label: '菲尼克斯' },
        'America/Toronto': { query: '多伦多', label: '多伦多' },
        'America/Sao_Paulo': { query: '圣保罗', label: '圣保罗' },
        'Australia/Sydney': { query: '悉尼', label: '悉尼' },
        'Australia/Melbourne': { query: '墨尔本', label: '墨尔本' },
        'Pacific/Auckland': { query: '奥克兰', label: '奥克兰' },
        'Africa/Johannesburg': { query: '约翰内斯堡', label: '约翰内斯堡' },
        'Etc/UTC': { query: 'London', label: 'UTC' }
    };

    let currentWeatherLocation = null;
    let currentWeatherSource = null;
    let currentEdgeTimezone = 'Asia/Shanghai';

    function normalizeEdgeTimezone(timezone) {
        if (!timezone) return 'Asia/Shanghai';
        return timezone;
    }

    function formatTimezoneLabel(timezone) {
        return normalizeEdgeTimezone(timezone).replace(/_/g, ' ');
    }

    function getWeatherSourceText(source) {
        const activeSource = source || currentWeatherSource;
        if (!activeSource) {
            return '边缘节点时区：' + formatTimezoneLabel(currentEdgeTimezone);
        }
        return '边缘节点时区：' + formatTimezoneLabel(activeSource.timezone) + ' · 映射城市：' + activeSource.label;
    }

    function updateWeatherSourceNote(source) {
        const noteEl = document.getElementById('weather-source-note');
        if (!noteEl) return;
        noteEl.innerHTML = '<strong>定位来源</strong> ' + getWeatherSourceText(source);
    }

    function getWeatherLookupByTimezone(timezone) {
        const normalized = normalizeEdgeTimezone(timezone);
        const mapped = EDGE_TIMEZONE_CITY_MAP[normalized];
        if (mapped) {
            return { timezone: normalized, query: mapped.query, label: mapped.label };
        }
        if (/^Etc\//.test(normalized) || normalized === 'UTC') {
            return { timezone: normalized, query: 'London', label: 'UTC' };
        }

        const parts = normalized.split('/');
        const leaf = (parts[parts.length - 1] || 'Shanghai').replace(/_/g, ' ');
        return { timezone: normalized, query: leaf, label: leaf };
    }

    // 初始化天气
    async function initWeather() {
        const cache = getWeatherCache();
        if (cache && cache.location && cache.now && cache.forecast) {
            currentEdgeTimezone = normalizeEdgeTimezone(cache.timezone || currentEdgeTimezone);
            currentWeatherLocation = cache.location;
            currentWeatherSource = cache.source || getWeatherLookupByTimezone(cache.timezone || currentEdgeTimezone);
            updateWeatherSourceNote(currentWeatherSource);
            renderWeatherMini(cache.now, cache.location);
            renderWeatherModal(cache.now, cache.forecast, cache.location);
        }
        await loadWeatherByEdgeTimezone();
    }

    // 获取缓存
    function getWeatherCache() {
        try {
            const data = localStorage.getItem(WEATHER_CACHE_KEY);
            if (!data) return null;
            const cache = JSON.parse(data);
            if (Date.now() - cache.timestamp > WEATHER_CACHE_DURATION) return null;
            return cache;
        } catch (e) { return null; }
    }

    // 设置缓存
    function setWeatherCache(location, now, forecast) {
        localStorage.setItem(WEATHER_CACHE_KEY, JSON.stringify({
            timezone: normalizeEdgeTimezone(currentEdgeTimezone),
            source: currentWeatherSource,
            location: location,
            now: now,
            forecast: forecast,
            timestamp: Date.now()
        }));
    }

    // 根据 Cloudflare 边缘节点时区加载天气
    async function loadWeatherByEdgeTimezone() {
        try {
            document.getElementById('weather-mini').innerHTML = '<span class="weather-loading">定位中...</span>';
            const edgeRes = await fetchWithTimeout(WEATHER_API + '/edge', {}, 8000);
            if (edgeRes.status === 503) {
                weatherNotConfigured = true;
                renderWeatherNotConfigured('--');
                return;
            }
            const edgeData = await edgeRes.json();
            if (edgeData.code !== '200' || !edgeData.location || !edgeData.now || !edgeData.forecast) {
                throw new Error(edgeData.error || '边缘天气查询失败');
            }

            currentEdgeTimezone = normalizeEdgeTimezone(edgeData.timezone || currentEdgeTimezone);
            currentWeatherSource = edgeData.source || getWeatherLookupByTimezone(currentEdgeTimezone);
            currentWeatherLocation = edgeData.location;

            updateWeatherSourceNote(currentWeatherSource);
            setWeatherCache(currentWeatherLocation, edgeData.now, edgeData.forecast);
            renderWeatherMini(edgeData.now, currentWeatherLocation);
            renderWeatherModal(edgeData.now, edgeData.forecast, currentWeatherLocation);
        } catch (e) {
            console.error('天气加载失败详细错误:', e);
            updateWeatherSourceNote(currentWeatherSource || getWeatherLookupByTimezone(currentEdgeTimezone));
            document.getElementById('weather-mini').innerHTML = '<span class="weather-loading" title="' + e.message + '">加载失败</span>';
        }
    }

    // 加载天气数据
    async function loadWeatherData() {
        if (!currentWeatherLocation) return;
        try {
            const [nowRes, forecastRes] = await Promise.all([
                fetch(WEATHER_API + '/now?location=' + currentWeatherLocation.id),
                fetch(WEATHER_API + '/3d?location=' + currentWeatherLocation.id)
            ]);

            // 检查是否未配置天气服务
            if (nowRes.status === 503 || forecastRes.status === 503) {
                weatherNotConfigured = true;
                renderWeatherNotConfigured(currentWeatherLocation.name);
                return;
            }

            const nowData = await nowRes.json();
            const forecastData = await forecastRes.json();

            if (nowData.code !== '200' || forecastData.code !== '200') throw new Error('天气API错误');

            setWeatherCache(currentWeatherLocation, nowData.now, forecastData.daily);
            renderWeatherMini(nowData.now, currentWeatherLocation);
            renderWeatherModal(nowData.now, forecastData.daily, currentWeatherLocation);
        } catch (e) {
            console.warn('天气数据加载失败:', e);
        }
    }

    // 渲染未配置天气服务的状态
    function renderWeatherNotConfigured(cityName) {
        // 迷你天气：城市 + --°
        updateWeatherSourceNote(currentWeatherSource || getWeatherLookupByTimezone(currentEdgeTimezone));
        document.getElementById('weather-mini').innerHTML =
            '<span class="weather-city">' + (cityName || '--') + '</span>' +
            '<span class="weather-temp">--°</span>';
        // 弹窗内容
        document.getElementById('weather-current').innerHTML =
            '<div class="weather-not-configured">未配置天气 Api Key</div>';
    }

    // 渲染迷你天气
    function renderWeatherMini(now, location) {
        const icon = WEATHER_ICONS[now.icon] || '❓';
        document.getElementById('weather-mini').title = location.name + ' · ' + now.text + ' · ' + now.temp + '°C · ' + getWeatherSourceText();
        document.getElementById('weather-mini').innerHTML =
            '<span class="weather-city">' + location.name + '</span>' +
            '<span class="weather-icon">' + icon + '</span>' +
            '<span class="weather-temp">' + now.temp + '°</span>';
    }

    function formatWeatherUpdateTime(obsTime) {
        if (!obsTime) return '';
        try {
            return new Date(obsTime).toLocaleTimeString('zh-CN', {
                hour: '2-digit',
                minute: '2-digit',
                hour12: false
            });
        } catch (error) {
            return '';
        }
    }

    // 渲染天气弹窗
    function renderWeatherModal(now, forecast, location) {
        const icon = WEATHER_ICONS[now.icon] || '❓';
        const updateTime = formatWeatherUpdateTime(now.obsTime);
        const detailSuffix = updateTime ? ' · 更新于 ' + updateTime : '';
        updateWeatherSourceNote(currentWeatherSource);
        document.getElementById('weather-current').innerHTML =
            '<div class="weather-current-icon">' + icon + '</div>' +
            '<div class="weather-current-temp">' + now.temp + '°C</div>' +
            '<div class="weather-current-desc">' + location.name + ' · ' + now.text + '</div>' +
            '<div class="weather-current-detail">体感' + now.feelsLike + '° 湿度' + now.humidity + '% ' + now.windDir + now.windScale + '级' + detailSuffix + '</div>';

        let forecastHtml = '';
        forecast.slice(0, 3).forEach(function(day, i) {
            var date = new Date(day.fxDate);
            var dayName = i === 0 ? '今天' : WEEKDAYS[date.getDay()];
            var dayIcon = WEATHER_ICONS[day.iconDay] || '❓';
            forecastHtml += '<div class="weather-forecast-item">' +
                '<div class="weather-forecast-day">' + dayName + '</div>' +
                '<div class="weather-forecast-icon">' + dayIcon + '</div>' +
                '<div class="weather-forecast-temp"><span class="low">' + day.tempMin + '°</span>~<span class="high">' + day.tempMax + '°</span></div>' +
            '</div>';
        });
        document.getElementById('weather-forecast').innerHTML = forecastHtml;
    }

    // 弹窗控制
    function openWeatherModal() {
        document.getElementById('weather-modal').classList.add('show');
    }
    function closeWeatherModal() {
        document.getElementById('weather-modal').classList.remove('show');
    }
    function closeWeatherModalOutside(e) {
        if (e.target.id === 'weather-modal') closeWeatherModal();
    }

    function bootWeatherApp() {
        if (weatherAppBooted) return;
        weatherAppBooted = true;
        initAmbientBackground();
        setTimeout(function() {
            console.log('开始初始化天气组件...');
            initWeather().catch(function(err) {
                console.error('天气初始化失败:', err);
                document.getElementById('weather-mini').innerHTML = '<span class="weather-loading">加载失败</span>';
            });
        }, 500); // 延迟加载，优先加载主内容
    }

    function bootApplication() {
        bootMainApp();
        bootWeatherApp();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', bootApplication, { once: true });
    } else {
        bootApplication();
    }

    // 兜底：即使 DOMContentLoaded 没按预期触发，也强制启动一次
    setTimeout(bootApplication, 1200);

    </script>

    <!-- 天气弹窗 -->
    <div class="weather-modal" id="weather-modal" onclick="closeWeatherModalOutside(event)">
        <div class="weather-modal-content" onclick="event.stopPropagation()">
            <div class="weather-modal-header">
                <span class="weather-modal-title">天气详情</span>
                <button class="weather-modal-close" onclick="closeWeatherModal()">&times;</button>
            </div>
            <div class="weather-source-note" id="weather-source-note"><strong>定位来源</strong> 边缘节点时区：--</div>
            <div class="weather-current" id="weather-current">
                <div class="weather-current-icon">--</div>
                <div class="weather-current-temp">--°C</div>
                <div class="weather-current-desc">--</div>
                <div class="weather-current-detail">体感--° 湿度--% --</div>
            </div>
            <div class="weather-forecast" id="weather-forecast">
                <div class="weather-forecast-item"><div class="weather-forecast-day">--</div><div class="weather-forecast-icon">--</div><div class="weather-forecast-temp">--</div></div>
                <div class="weather-forecast-item"><div class="weather-forecast-day">--</div><div class="weather-forecast-icon">--</div><div class="weather-forecast-temp">--</div></div>
                <div class="weather-forecast-item"><div class="weather-forecast-day">--</div><div class="weather-forecast-icon">--</div><div class="weather-forecast-temp">--</div></div>
            </div>
        </div>
    </div>
</body>

</html>
`;

// 常量时间比较函数，防止时序攻击
function constantTimeCompare(a, b) {
    if (a.length !== b.length) return false;
    let result = 0;
    for (let i = 0; i < a.length; i++) {
        result |= a.charCodeAt(i) ^ b.charCodeAt(i);
    }
    return result === 0;
}

// 服务端 token 验证
async function validateServerToken(authToken, env) {
    if (!authToken) {
        return {
            isValid: false,
            status: 401,
            response: { error: 'Unauthorized', message: '未登录或登录已过期' }
        };
    }

    try {
        const parts = authToken.split('.');
        let timestamp, expiryMinutes, hash;

        // 兼容新旧两种 token 格式
        if (parts.length === 3) {
            // 新格式：timestamp.expiryMinutes.hash
            [timestamp, expiryMinutes, hash] = parts;
            expiryMinutes = parseInt(expiryMinutes);
            if (isNaN(expiryMinutes)) {
                throw new Error('Invalid expiry format');
            }
        } else if (parts.length === 2) {
            // 旧格式：timestamp.hash（向后兼容，使用默认30分钟）
            [timestamp, hash] = parts;
            const envExpiry = parseInt(env.TOKEN_EXPIRY_MINUTES);
            expiryMinutes = isNaN(envExpiry) ? 30 : envExpiry;
        } else {
            throw new Error('Invalid token format');
        }

        const tokenTimestamp = parseInt(timestamp);
        const now = Date.now();

        // 计算过期时间：-1 表示永久有效
        if (expiryMinutes !== -1) {
            const expiryMs = expiryMinutes * 60 * 1000;
            if (now - tokenTimestamp > expiryMs) {
                return {
                    isValid: false,
                    status: 401,
                    response: {
                        error: 'Token expired',
                        tokenExpired: true,
                        message: '登录已过期，请重新登录'
                    }
                };
            }
        }

        // 根据 token 格式构建验证数据
        const tokenData = parts.length === 3
            ? timestamp + "_" + expiryMinutes + "_" + env.ADMIN_PASSWORD
            : timestamp + "_" + env.ADMIN_PASSWORD;
        const encoder = new TextEncoder();
        const data = encoder.encode(tokenData);
        const hashBuffer = await crypto.subtle.digest('SHA-256', data);
        const expectedHash = btoa(String.fromCharCode(...new Uint8Array(hashBuffer)));

        // 使用常量时间比较防止时序攻击
        if (!constantTimeCompare(hash, expectedHash)) {
            return {
                isValid: false,
                status: 401,
                response: {
                    error: 'Invalid token',
                    tokenInvalid: true,
                    message: '登录状态无效，请重新登录'
                }
            };
        }

        return { isValid: true };
    } catch (error) {
        // 避免泄露详细错误信息
        return {
            isValid: false,
            status: 401,
            response: {
                error: 'Invalid token',
                tokenInvalid: true,
                message: '登录验证失败，请重新登录'
            }
        };
    }
}

// 管理员权限验证函数
async function validateAdminToken(authToken, env) {
    const validation = await validateServerToken(authToken, env);
    if (!validation.isValid) {
        return validation;
    }

    // Token有效，确认管理员权限
    return {
        isValid: true,
        isAdmin: true
    };
}

export default {
    async fetch(request, env) {
      const url = new URL(request.url);
      const jsonHeaders = { 'Content-Type': 'application/json' };
      const createJsonResponse = (payload, status = 200) =>
        new Response(JSON.stringify(payload), { status, headers: jsonHeaders });
      const createEmptyLinksPayload = (warning) => {
        const payload = { links: [], categories: {} };
        if (warning) {
          payload.warning = warning;
        }
        return payload;
      };
      const withTimeout = async (promise, timeoutMs, errorMessage) => {
        let timer = null;
        try {
          return await Promise.race([
            promise,
            new Promise((_, reject) => {
              timer = setTimeout(() => reject(new Error(errorMessage || 'operation_timeout')), timeoutMs);
            })
          ]);
        } finally {
          if (timer) clearTimeout(timer);
        }
      };

      if (url.pathname === '/') {
        return new Response(HTML_CONTENT, {
          headers: { 'Content-Type': 'text/html' }
        });
      }

      // ========== 天气代理 API ==========
      const normalizeBaseUrl = (raw) => {
        const value = (raw || '').trim();
        if (!value) return null;
        const withScheme = /^https?:\/\//i.test(value) ? value : `https://${value}`;
        return withScheme.replace(/\/+$/, '');
      };

      // 公告说明：公共 API 域名将逐步停服，建议改用你的 API Host（形如：xxx.yyy.qweatherapi.com）
      // - devapi.qweather.com 已于 2026-01-01 停止服务
      // - GeoAPI 使用 API Host 时需从 /v2/... 变更为 /geo/v2/...
      const qweatherHost = normalizeBaseUrl(env.WEATHER_API_HOST);
      const qweatherApiBase = qweatherHost || 'https://api.qweather.com';
      const qweatherGeoBase = qweatherHost ? `${qweatherApiBase}/geo` : 'https://geoapi.qweather.com';
      const edgeTimezoneCityMap = {
        'Asia/Shanghai': { query: '上海', label: '上海' },
        'Asia/Hong_Kong': { query: '香港', label: '香港' },
        'Asia/Tokyo': { query: '东京', label: '东京' },
        'Asia/Seoul': { query: '首尔', label: '首尔' },
        'Asia/Singapore': { query: '新加坡', label: '新加坡' },
        'Asia/Bangkok': { query: '曼谷', label: '曼谷' },
        'Asia/Kuala_Lumpur': { query: '吉隆坡', label: '吉隆坡' },
        'Asia/Jakarta': { query: '雅加达', label: '雅加达' },
        'Asia/Kolkata': { query: '孟买', label: '孟买' },
        'Asia/Dubai': { query: '迪拜', label: '迪拜' },
        'Europe/London': { query: '伦敦', label: '伦敦' },
        'Europe/Paris': { query: '巴黎', label: '巴黎' },
        'Europe/Berlin': { query: '柏林', label: '柏林' },
        'Europe/Madrid': { query: '马德里', label: '马德里' },
        'Europe/Moscow': { query: '莫斯科', label: '莫斯科' },
        'America/New_York': { query: '纽约', label: '纽约' },
        'America/Chicago': { query: '芝加哥', label: '芝加哥' },
        'America/Denver': { query: '丹佛', label: '丹佛' },
        'America/Los_Angeles': { query: '洛杉矶', label: '洛杉矶' },
        'America/Phoenix': { query: '菲尼克斯', label: '菲尼克斯' },
        'America/Toronto': { query: '多伦多', label: '多伦多' },
        'America/Sao_Paulo': { query: '圣保罗', label: '圣保罗' },
        'Australia/Sydney': { query: '悉尼', label: '悉尼' },
        'Australia/Melbourne': { query: '墨尔本', label: '墨尔本' },
        'Pacific/Auckland': { query: '奥克兰', label: '奥克兰' },
        'Africa/Johannesburg': { query: '约翰内斯堡', label: '约翰内斯堡' },
        'Etc/UTC': { query: 'London', label: 'UTC' }
      };
      const normalizeEdgeTimezone = (timezone) => timezone || 'Asia/Shanghai';
      const getWeatherLookupByTimezone = (timezone) => {
        const normalized = normalizeEdgeTimezone(timezone);
        const mapped = edgeTimezoneCityMap[normalized];
        if (mapped) {
          return { timezone: normalized, query: mapped.query, label: mapped.label };
        }
        if (/^Etc\//.test(normalized) || normalized === 'UTC') {
          return { timezone: normalized, query: 'London', label: 'UTC' };
        }
        const parts = normalized.split('/');
        const leaf = (parts[parts.length - 1] || 'Shanghai').replace(/_/g, ' ');
        return { timezone: normalized, query: leaf, label: leaf };
      };

      const proxyQWeatherJson = async (targetUrl) => {
        const redactedUrl = targetUrl.replace(/([?&]key=)[^&]*/i, '$1***');
        let res;
        try {
          res = await fetch(targetUrl);
        } catch (err) {
          return new Response(JSON.stringify({
            code: '502',
            error: 'qweather_fetch_failed',
            upstreamUrl: redactedUrl,
            message: String(err)
          }), { status: 502, headers: { 'Content-Type': 'application/json' } });
        }

        const contentType = res.headers.get('content-type') || '';
        const text = await res.text();

        if (!res.ok) {
          return new Response(JSON.stringify({
            code: String(res.status),
            error: 'qweather_upstream_error',
            upstreamStatus: res.status,
            upstreamContentType: contentType,
            upstreamUrl: redactedUrl,
            upstreamBody: (text || '').slice(0, 800)
          }), { status: 502, headers: { 'Content-Type': 'application/json' } });
        }

        try {
          if (!text) throw new Error('empty response body');
          const data = JSON.parse(text);
          return new Response(JSON.stringify(data), { headers: { 'Content-Type': 'application/json' } });
        } catch (err) {
          return new Response(JSON.stringify({
            code: '502',
            error: 'qweather_invalid_json',
            upstreamContentType: contentType,
            upstreamUrl: redactedUrl,
            upstreamBody: (text || '').slice(0, 800),
            message: String(err)
          }), { status: 502, headers: { 'Content-Type': 'application/json' } });
        }
      };

      if (url.pathname === '/api/weather/edge') {
        if (!env.WEATHER_API_KEY) {
          return createJsonResponse({ code: '503', error: 'weather_not_configured' }, 503);
        }

        const edgeTimezone = normalizeEdgeTimezone(request.cf && request.cf.timezone);
        const source = getWeatherLookupByTimezone(edgeTimezone);
        const geoRes = await proxyQWeatherJson(
          `${qweatherGeoBase}/v2/city/lookup?location=${encodeURIComponent(source.query)}&key=${env.WEATHER_API_KEY}&number=1`
        );
        if (geoRes.status !== 200) {
          return geoRes;
        }

        const geoData = await geoRes.json();
        if (geoData.code !== '200' || !geoData.location || !geoData.location.length) {
          return new Response(JSON.stringify({
            code: '502',
            error: 'edge_city_lookup_failed',
            timezone: edgeTimezone,
            source
          }), { status: 502, headers: jsonHeaders });
        }

        const location = geoData.location[0];
        const [nowRes, forecastRes] = await Promise.all([
          proxyQWeatherJson(
            `${qweatherApiBase}/v7/weather/now?location=${encodeURIComponent(location.id)}&key=${env.WEATHER_API_KEY}`
          ),
          proxyQWeatherJson(
            `${qweatherApiBase}/v7/weather/3d?location=${encodeURIComponent(location.id)}&key=${env.WEATHER_API_KEY}`
          )
        ]);

        if (nowRes.status !== 200) return nowRes;
        if (forecastRes.status !== 200) return forecastRes;

        const nowData = await nowRes.json();
        const forecastData = await forecastRes.json();
        if (nowData.code !== '200' || forecastData.code !== '200') {
          return new Response(JSON.stringify({
            code: '502',
            error: 'edge_weather_lookup_failed',
            timezone: edgeTimezone,
            source
          }), { status: 502, headers: jsonHeaders });
        }

        return createJsonResponse({
          code: '200',
          timezone: edgeTimezone,
          source,
          location,
          now: nowData.now,
          forecast: forecastData.daily
        });
      }

      if (url.pathname === '/api/weather/now') {
        if (!env.WEATHER_API_KEY) {
          return createJsonResponse({ code: '503', error: 'weather_not_configured' }, 503);
        }
        const location = url.searchParams.get('location');
        if (!location) {
          return createJsonResponse({ code: '400', error: 'missing location' }, 400);
        }
        return await proxyQWeatherJson(
          `${qweatherApiBase}/v7/weather/now?location=${encodeURIComponent(location)}&key=${env.WEATHER_API_KEY}`
        );
      }

      if (url.pathname === '/api/weather/3d') {
        if (!env.WEATHER_API_KEY) {
          return createJsonResponse({ code: '503', error: 'weather_not_configured' }, 503);
        }
        const location = url.searchParams.get('location');
        if (!location) {
          return createJsonResponse({ code: '400', error: 'missing location' }, 400);
        }
        return await proxyQWeatherJson(
          `${qweatherApiBase}/v7/weather/3d?location=${encodeURIComponent(location)}&key=${env.WEATHER_API_KEY}`
        );
      }

      if (url.pathname === '/api/weather/geo') {
        if (!env.WEATHER_API_KEY) {
          return createJsonResponse({ code: '503', error: 'weather_not_configured' }, 503);
        }
        const location = url.searchParams.get('location');
        const number = url.searchParams.get('number') || '8';
        if (!location) {
          return createJsonResponse({ code: '400', error: 'missing location' }, 400);
        }
        return await proxyQWeatherJson(
          `${qweatherGeoBase}/v2/city/lookup?location=${encodeURIComponent(location)}&key=${env.WEATHER_API_KEY}&number=${number}`
        );
      }

      if (url.pathname === '/api/getLinks') {
        const userId = url.searchParams.get('userId') || 'testUser';
        const authToken = request.headers.get('Authorization');
        if (!env.CARD_ORDER || typeof env.CARD_ORDER.get !== 'function') {
          return createJsonResponse(createEmptyLinksPayload('card_order_unbound'));
        }

        let data = null;
        try {
          data = await withTimeout(env.CARD_ORDER.get(userId), 3500, 'card_order_get_timeout');
        } catch (error) {
          return createJsonResponse(createEmptyLinksPayload(error.message || 'card_order_get_failed'));
        }

        if (data) {
            let parsedData;
            try {
                parsedData = JSON.parse(data);
            } catch (error) {
                return createJsonResponse(createEmptyLinksPayload('card_order_invalid_json'));
            }

            // 验证 token
            if (authToken) {
                const validation = await validateServerToken(authToken, env);
                if (!validation.isValid) {
                    return createJsonResponse(validation.response, validation.status);
                }

                // Token 有效，返回完整数据
                return createJsonResponse(parsedData);
            }

            // 未提供 token，只返回公开数据
            const parsedLinks = Array.isArray(parsedData.links) ? parsedData.links : [];
            const parsedCategories = parsedData.categories && typeof parsedData.categories === 'object' ? parsedData.categories : {};
            const filteredLinks = parsedLinks.filter(link => !link.isPrivate);
            const filteredCategories = {};
            Object.keys(parsedCategories).forEach(category => {
                const categoryLinks = Array.isArray(parsedCategories[category]) ? parsedCategories[category] : [];
                filteredCategories[category] = categoryLinks.filter(link => !link.isPrivate);
            });

            return createJsonResponse({
                links: filteredLinks,
                categories: filteredCategories
            });
        }

        return createJsonResponse(createEmptyLinksPayload());
      }

      if (url.pathname === '/api/saveOrder' && request.method === 'POST') {
        const authToken = request.headers.get('Authorization');
        const validation = await validateServerToken(authToken, env);

        if (!validation.isValid) {
            return new Response(JSON.stringify(validation.response), {
                status: validation.status,
                headers: { 'Content-Type': 'application/json' }
            });
        }

        const { userId, links, categories } = await request.json();
        await env.CARD_ORDER.put(userId, JSON.stringify({ links, categories }));
        return new Response(JSON.stringify({
            success: true,
            message: '保存成功'
        }), {
            status: 200,
            headers: { 'Content-Type': 'application/json' }
        });
      }

      if (url.pathname === '/api/verifyPassword' && request.method === 'POST') {
        try {
            const body = await request.json();
            const { password, expiryMinutes: clientExpiry } = body;
            const isValid = password === env.ADMIN_PASSWORD;

            if (isValid) {
                // 允许的有效期白名单（分钟）：15分钟、1小时、1天、7天、30天、永久
                const ALLOWED_EXPIRY_VALUES = [15, 60, 1440, 10080, 43200, -1];
                const defaultExpiry = parseInt(env.TOKEN_EXPIRY_MINUTES) || 30;

                // 验证客户端传入的有效期是否在白名单中
                let expiryMinutes = defaultExpiry;
                if (typeof clientExpiry === 'number' && Number.isInteger(clientExpiry) && ALLOWED_EXPIRY_VALUES.includes(clientExpiry)) {
                    expiryMinutes = clientExpiry;
                }

                // 生成包含时间戳和有效期的加密 token
                const timestamp = Date.now();
                const tokenData = timestamp + "_" + expiryMinutes + "_" + env.ADMIN_PASSWORD;
                const encoder = new TextEncoder();
                const data = encoder.encode(tokenData);
                const hashBuffer = await crypto.subtle.digest('SHA-256', data);

                // 使用指定格式：timestamp.expiryMinutes.hash
                const token = timestamp + "." + expiryMinutes + "." + btoa(String.fromCharCode(...new Uint8Array(hashBuffer)));

                return new Response(JSON.stringify({
                    valid: true,
                    token: token,
                    expiryMinutes: expiryMinutes
                }), {
                    status: 200,
                    headers: { 'Content-Type': 'application/json' }
                });
            }

            return new Response(JSON.stringify({
                valid: false,
                error: 'Invalid password'
            }), {
                status: 403,
                headers: { 'Content-Type': 'application/json' }
            });
        } catch (error) {
            return new Response(JSON.stringify({
                valid: false,
                error: error.message
            }), {
                status: 500,
                headers: { 'Content-Type': 'application/json' }
            });
        }
      }

      if (url.pathname === '/api/backupData' && request.method === 'POST') {
        // 🔧 安全修复：添加管理员权限验证
        const authToken = request.headers.get('Authorization');
        const validation = await validateAdminToken(authToken, env);

        if (!validation.isValid) {
            return new Response(JSON.stringify(validation.response), {
                status: validation.status,
                headers: { 'Content-Type': 'application/json' }
            });
        }

        try {
            const { sourceUserId } = await request.json();
            const result = await this.backupData(env, sourceUserId);
            return new Response(JSON.stringify(result), {
              status: result.success ? 200 : 404,
              headers: { 'Content-Type': 'application/json' }
            });
        } catch (error) {
            // 避免泄露详细错误信息
            return new Response(JSON.stringify({
                success: false,
                message: '备份操作失败'
            }), {
                status: 500,
                headers: { 'Content-Type': 'application/json' }
            });
        }
      }

      return new Response('Not Found', { status: 404 });
    },

    async backupData(env, sourceUserId) {
        const MAX_BACKUPS = 10;
        const sourceData = await env.CARD_ORDER.get(sourceUserId);

        if (sourceData) {
            try {
                const currentDate = new Date().toLocaleString('zh-CN', {
                    timeZone: 'Asia/Shanghai',
                    year: 'numeric',
                    month: '2-digit',
                    day: '2-digit',
                    hour: '2-digit',
                    minute: '2-digit',
                    second: '2-digit',
                    hour12: false
                }).replace(/\//g, '-');

                const backupId = `backup_${currentDate}`;

                const backups = await env.CARD_ORDER.list({ prefix: 'backup_' });
                const backupKeys = backups.keys.map(key => key.name).sort((a, b) => {
                    const timeA = new Date(a.split('_')[1].replace(/-/g, '/')).getTime();
                    const timeB = new Date(b.split('_')[1].replace(/-/g, '/')).getTime();
                    return timeB - timeA;  // 降序排序，最新的在前
                });

                await env.CARD_ORDER.put(backupId, sourceData);

                const allBackups = [...backupKeys, backupId].sort((a, b) => {
                    const timeA = new Date(a.split('_')[1].replace(/-/g, '/')).getTime();
                    const timeB = new Date(b.split('_')[1].replace(/-/g, '/')).getTime();
                    return timeB - timeA;
                });

                const backupsToDelete = allBackups.slice(MAX_BACKUPS);

                if (backupsToDelete.length > 0) {
                    await Promise.all(
                        backupsToDelete.map(key => env.CARD_ORDER.delete(key))
                    );
                }

                return {
                    success: true,
                    backupId,
                    remainingBackups: MAX_BACKUPS,
                    deletedCount: backupsToDelete.length
                };
            } catch (error) {
                return {
                    success: false,
                    error: 'Backup operation failed',
                    details: error.message
                };
            }
        }
        return { success: false, error: 'Source data not found' };
    }
  };
