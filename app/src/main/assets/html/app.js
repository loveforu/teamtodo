/**
 * TeamTodo JavaScript Bridge
 * Stitch HTML ?붾㈃怨?Android Native ?ъ씠???듭떊 ?덉씠?? * 
 * ?ъ슜踰?
 *   TeamTodo.navigate('todo_main')
 *   TeamTodo.getTodos('profileId', '2024-03-01', callback)
 *   TeamTodo.saveTodo({...}, callback)
 */

(function () {
    'use strict';

    (function installPrepaintGuard() {
        try {
            const root = document.documentElement;
            root.classList.add('tt-preparing');
            root.classList.add('tt-no-transitions');
            if (!document.getElementById('teamtodo-prepaint-guard')) {
                const style = document.createElement('style');
                style.id = 'teamtodo-prepaint-guard';
                style.textContent = `
                    html.tt-preparing body { opacity: 0 !important; }
                    html.tt-ready body { opacity: 1 !important; transition: opacity .12s ease; }
                    html.tt-no-transitions *,
                    html.tt-no-transitions *::before,
                    html.tt-no-transitions *::after {
                        transition: none !important;
                        animation: none !important;
                    }
                `;
                (document.head || document.documentElement).appendChild(style);
            }
        } catch (_) {}
    })();

    function buildThemeOverrideCss(normalized, light, rgb) {
        return `
            :root {
                --theme-color: ${normalized};
                --primary-color: ${normalized};
                --primary-light: ${light};
                --teamtodo-primary-rgb: ${rgb.r} ${rgb.g} ${rgb.b};
            }
            .bg-primary { background-color: rgb(var(--teamtodo-primary-rgb) / 1) !important; }
            .bg-primary\\/5 { background-color: rgb(var(--teamtodo-primary-rgb) / 0.05) !important; }
            .bg-primary\\/10 { background-color: rgb(var(--teamtodo-primary-rgb) / 0.10) !important; }
            .bg-primary\\/20 { background-color: rgb(var(--teamtodo-primary-rgb) / 0.20) !important; }
            .bg-primary\\/30 { background-color: rgb(var(--teamtodo-primary-rgb) / 0.30) !important; }
            .bg-primary\\/40 { background-color: rgb(var(--teamtodo-primary-rgb) / 0.40) !important; }
            .bg-primary\\/50 { background-color: rgb(var(--teamtodo-primary-rgb) / 0.50) !important; }
            .bg-primary\\/80 { background-color: rgb(var(--teamtodo-primary-rgb) / 0.80) !important; }
            .bg-primary\\/90 { background-color: rgb(var(--teamtodo-primary-rgb) / 0.90) !important; }
            .bg-primary\\/\\[0\\.04\\] { background-color: rgb(var(--teamtodo-primary-rgb) / 0.04) !important; }
            .bg-primary\\/\\[0\\.08\\] { background-color: rgb(var(--teamtodo-primary-rgb) / 0.08) !important; }
            .text-primary { color: rgb(var(--teamtodo-primary-rgb) / 1) !important; }
            .text-primary\\/60 { color: rgb(var(--teamtodo-primary-rgb) / 0.60) !important; }
            .border-primary { border-color: rgb(var(--teamtodo-primary-rgb) / 1) !important; }
            .border-primary\\/10 { border-color: rgb(var(--teamtodo-primary-rgb) / 0.10) !important; }
            .border-primary\\/20 { border-color: rgb(var(--teamtodo-primary-rgb) / 0.20) !important; }
            .border-primary\\/30 { border-color: rgb(var(--teamtodo-primary-rgb) / 0.30) !important; }
            .border-primary\\/40 { border-color: rgb(var(--teamtodo-primary-rgb) / 0.40) !important; }
            .from-primary { --tw-gradient-from: rgb(var(--teamtodo-primary-rgb) / 1) var(--tw-gradient-from-position) !important; }
            .to-primary { --tw-gradient-to: rgb(var(--teamtodo-primary-rgb) / 1) var(--tw-gradient-to-position) !important; }
            .to-primary\\/80 { --tw-gradient-to: rgb(var(--teamtodo-primary-rgb) / 0.80) var(--tw-gradient-to-position) !important; }
            .shadow-primary\\/10 { --tw-shadow-color: rgb(var(--teamtodo-primary-rgb) / 0.10) !important; }
            .shadow-primary\\/20 { --tw-shadow-color: rgb(var(--teamtodo-primary-rgb) / 0.20) !important; }
            .shadow-primary\\/30 { --tw-shadow-color: rgb(var(--teamtodo-primary-rgb) / 0.30) !important; }
            .shadow-primary\\/40 { --tw-shadow-color: rgb(var(--teamtodo-primary-rgb) / 0.40) !important; }
            .ring-primary\\/10 { --tw-ring-color: rgb(var(--teamtodo-primary-rgb) / 0.10) !important; }
            .ring-primary\\/20 { --tw-ring-color: rgb(var(--teamtodo-primary-rgb) / 0.20) !important; }
            .decoration-primary\\/30 { text-decoration-color: rgb(var(--teamtodo-primary-rgb) / 0.30) !important; }
            .decoration-primary\\/50 { text-decoration-color: rgb(var(--teamtodo-primary-rgb) / 0.50) !important; }
        `;
    }

    function ensureThemeOverrideStyle(normalized, light, rgb) {
        const styleId = 'teamtodo-theme';
        let styleEl = document.getElementById(styleId);
        if (!styleEl) {
            styleEl = document.createElement('style');
            styleEl.id = styleId;
            const root = document.head || document.documentElement;
            root.appendChild(styleEl);
        }
        styleEl.textContent = buildThemeOverrideCss(normalized, light, rgb);
    }

    // Apply saved theme as early as possible to prevent transition flicker.
    (function bootstrapThemeEarly() {
        try {
            const normalize = (input) => {
                if (!input) return '#EB4770';
                const raw = String(input).trim();
                const m = raw.match(/^#?([0-9a-fA-F]{6})$/);
                if (!m) return '#EB4770';
                return ('#' + m[1]).toUpperCase();
            };
            const hexToRgbLocal = (hex) => {
                const n = normalize(hex).slice(1);
                return {
                    r: parseInt(n.slice(0, 2), 16),
                    g: parseInt(n.slice(2, 4), 16),
                    b: parseInt(n.slice(4, 6), 16)
                };
            };

            const savedColor = normalize(localStorage.getItem('themeColor') || '#FF6B9D');
            const savedLight = localStorage.getItem('themeLight');
            const light = savedLight && /^#[0-9a-fA-F]{6}$/.test(savedLight)
                ? savedLight
                : savedColor + '1A';
            const rgb = hexToRgbLocal(savedColor);

            const root = document.documentElement;
            root.style.setProperty('--theme-color', savedColor);
            root.style.setProperty('--theme-color-light', light);
            root.style.setProperty('--primary-color', savedColor);
            root.style.setProperty('--primary-light', light);
            root.style.setProperty('--teamtodo-primary-rgb', `${rgb.r} ${rgb.g} ${rgb.b}`);
            ensureThemeOverrideStyle(savedColor, light, rgb);

            if (localStorage.getItem('darkMode') === 'true') root.classList.add('dark');
            else root.classList.remove('dark');
        } catch (_) {
        }
    })();

    // =============================
    // ?곹깭 愿由?    // =============================
    const state = {
        currentProfile: null,
        currentUser: null,
        themeColor: '#FF6B9D',
        pendingCallbacks: {},
        callbackId: 0
    };

    // =============================
    // Android Bridge ?몄텧
    // =============================
    function nativeCall(method, data, callback) {
        if (callback) {
            const cbId = 'cb_' + (++state.callbackId);
            state.pendingCallbacks[cbId] = callback;
            data = data || {};
            data._cbId = cbId;
        }
        if (window.AndroidBridge) {
            AndroidBridge.call(method, JSON.stringify(data || {}));
        } else {
            // 媛쒕컻 紐⑤뱶 (釉뚮씪?곗??먯꽌 ?뚯뒪????
            console.log('[TeamTodo] Native call:', method, data);
            if (callback) callback(null);
        }
    }

    // Native?먯꽌 ?대깽???섏떊
    window.onNativeEvent = function (eventName, data) {
        // 肄쒕갚 泥섎━
        if (data && data._cbId && state.pendingCallbacks[data._cbId]) {
            const cb = state.pendingCallbacks[data._cbId];
            delete state.pendingCallbacks[data._cbId];
            cb(data);
            return;
        }

        // ?대깽???붿뒪?⑥튂
        const event = new CustomEvent('teamtodo:' + eventName, { detail: data });
        document.dispatchEvent(event);

        // ?뱀닔 ?대깽??泥섎━
        switch (eventName) {
            case 'onLoginSuccess':
                state.currentUser = data;
                TeamTodo.navigate('profile_select');
                break;
            case 'onCurrentUser':
                state.currentUser = data;
                break;
            case 'onTodoCompleted':
                handleTodoCompleted(data);
                break;
            case 'onThumbsUpAdded':
                showThumbsUpAnimation();
                break;
        }
    };

    // URL 怨듭쑀 ?섏떊
    window.onSharedUrl = function (url) {
        try {
            if (url) localStorage.setItem('sharedUrlPending', String(url));
        } catch (_) {}
        TeamTodo.navigate('url_share');
    };

    // =============================
    // ?ъ씤???좊땲硫붿씠??    // =============================
    function handleTodoCompleted(data) {
        if (data.completed) {
            animateHeartToPointBar(data.points);
        } else {
            // 痍⑥냼 ???ъ씤??媛먯냼 ?좊땲硫붿씠??            updatePointBar(-data.points, false);
        }
    }

    function currentThemeColor() {
        const cssColor = getComputedStyle(document.documentElement).getPropertyValue('--primary-color').trim();
        if (cssColor && cssColor !== 'var(--primary-color)') return cssColor;
        return state.themeColor || '#EB4770';
    }

    function animateHeartToPointBar(points) {
        // ?섑듃 ?섏꽑???좎븘媛湲??좊땲硫붿씠??        const heart = document.createElement('div');
        heart.textContent = '❤';
        heart.style.cssText = `
            position: fixed;
            font-size: 28px;
            color: ${currentThemeColor()};
            z-index: 9999;
            pointer-events: none;
            transition: transform 1.2s cubic-bezier(0.25, 0.46, 0.45, 0.94),
                        opacity 0.3s ease;
            left: 20px;
            bottom: 100px;
        `;
        document.body.appendChild(heart);

        // ?ъ씤??諛??꾩튂濡??좎븘媛湲?(?섏꽑??寃쎈줈)
        const pointBar = document.querySelector('.point-bar, [class*="point"], #pointBar');
        const targetX = pointBar ? pointBar.getBoundingClientRect().left : window.innerWidth / 2;
        const targetY = pointBar ? pointBar.getBoundingClientRect().top : 80;

        let angle = 0;
        let progress = 0;
        const duration = 1200;
        const startTime = Date.now();

        function animate() {
            progress = Math.min((Date.now() - startTime) / duration, 1);
            angle += 15;
            const spiral = Math.sin(angle * Math.PI / 180) * (1 - progress) * 40;
            const x = 20 + (targetX - 20) * progress + spiral;
            const y = (window.innerHeight - 100) - ((window.innerHeight - 100) - targetY) * progress;

            heart.style.left = x + 'px';
            heart.style.top = y + 'px';
            heart.style.transform = `rotate(${angle}deg) scale(${1 - progress * 0.3})`;

            if (progress < 1) {
                requestAnimationFrame(animate);
            } else {
                // ?ъ씤??諛붿뿉 ?꾩갑 - ?섑듃 ?됱튌 & ?ъ씤?몃컮 ?낅뜲?댄듃
                heart.style.opacity = '0';
                fillPointBarHeart();
                updatePointBar(points, true);
                setTimeout(() => heart.remove(), 300);
            }
        }
        requestAnimationFrame(animate);
    }

    function fillPointBarHeart() {
        const heartIcon = document.querySelector('.heart-icon, [class*="heart"], #heartIcon');
        if (heartIcon) {
            heartIcon.classList.add('filled');
            // 源쒕묀???④낵
            heartIcon.style.animation = 'heartBlink 0.5s ease 2';
            if (!document.querySelector('#heart-anim-style')) {
                const style = document.createElement('style');
                style.id = 'heart-anim-style';
                style.textContent = `
                    @keyframes heartBlink {
                        0%, 100% { transform: scale(1); filter: brightness(1); }
                        50% { transform: scale(1.3); filter: brightness(1.5); }
                    }
                    .heart-icon.filled { color: var(--primary-color) !important; }
                `;
                document.head.appendChild(style);
            }
        }
    }

    function updatePointBar(points, increase) {
        const pointBar = document.querySelector('.point-bar, progress, [role="progressbar"]');
        if (pointBar) {
            const current = parseInt(pointBar.value || pointBar.getAttribute('aria-valuenow') || 0);
            const newVal = Math.max(0, Math.min(100, current + points));
            pointBar.value = newVal;
            pointBar.setAttribute('aria-valuenow', newVal);

            // 100???ъ꽦 ????＝
            if (newVal >= 100 && increase) {
                triggerConfetti();
            }
        }
        // ?ъ씤???レ옄 ?낅뜲?댄듃
        const pointText = document.querySelector('.point-text, [id*="point"], [class*="point-value"]');
        if (pointText) {
            const current = parseInt(pointText.textContent) || 0;
            pointText.textContent = Math.max(0, current + points);
        }
    }

    function triggerConfetti() {
        // 媛꾨떒????＝ ?④낵
        const colors = ['#FF6B9D', '#FFB3C6', '#FFD700', '#FF4757', '#5352ED'];
        for (let i = 0; i < 60; i++) {
            const confetti = document.createElement('div');
            confetti.style.cssText = `
                position: fixed;
                width: ${Math.random() * 10 + 5}px;
                height: ${Math.random() * 10 + 5}px;
                background: ${colors[Math.floor(Math.random() * colors.length)]};
                border-radius: ${Math.random() > 0.5 ? '50%' : '2px'};
                left: ${Math.random() * 100}vw;
                top: -20px;
                z-index: 9998;
                pointer-events: none;
                animation: confettiFall ${Math.random() * 2 + 1}s ease forwards;
            `;
            document.body.appendChild(confetti);
            setTimeout(() => confetti.remove(), 3000);
        }
        if (!document.querySelector('#confetti-style')) {
            const style = document.createElement('style');
            style.id = 'confetti-style';
            style.textContent = `
                @keyframes confettiFall {
                    0% { transform: translateY(0) rotate(0deg); opacity: 1; }
                    100% { transform: translateY(100vh) rotate(${Math.random() * 720}deg); opacity: 0; }
                }
            `;
            document.head.appendChild(style);
        }
    }

    function showThumbsUpAnimation() {
        const anim = document.createElement('div');
        anim.innerHTML = '?몟 +1';
        anim.style.cssText = `
            position: fixed;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            font-size: 36px;
            font-weight: bold;
            color: #FF6B9D;
            z-index: 9999;
            pointer-events: none;
            animation: thumbsUpAnim 1s ease forwards;
        `;
        document.body.appendChild(anim);
        if (!document.querySelector('#thumbs-style')) {
            const style = document.createElement('style');
            style.id = 'thumbs-style';
            style.textContent = `
                @keyframes thumbsUpAnim {
                    0% { opacity: 0; transform: translate(-50%, -50%) scale(0.5); }
                    50% { opacity: 1; transform: translate(-50%, -80%) scale(1.2); }
                    100% { opacity: 0; transform: translate(-50%, -120%) scale(1); }
                }
            `;
            document.head.appendChild(style);
        }
        setTimeout(() => anim.remove(), 1000);
    }

    // =============================
    // ?뚮쭏 ?곸슜
    // =============================
    function normalizeHexColor(input) {
        if (!input) return '#EB4770';
        const raw = String(input).trim();
        const m = raw.match(/^#?([0-9a-fA-F]{6})$/);
        if (!m) return '#EB4770';
        return ('#' + m[1]).toUpperCase();
    }

    function hexToRgb(hex) {
        const n = normalizeHexColor(hex).slice(1);
        return {
            r: parseInt(n.slice(0, 2), 16),
            g: parseInt(n.slice(2, 4), 16),
            b: parseInt(n.slice(4, 6), 16)
        };
    }

    function applyTheme(color) {
        const normalized = normalizeHexColor(color);
        const rgb = hexToRgb(normalized);
        const savedLight = localStorage.getItem('themeLight');
        const light = savedLight && /^#[0-9a-fA-F]{6}$/.test(savedLight) ? savedLight : normalized + '1A';

        state.themeColor = normalized;
        document.documentElement.style.setProperty('--theme-color', normalized);
        document.documentElement.style.setProperty('--theme-color-light', light);
        document.documentElement.style.setProperty('--primary-color', normalized);
        document.documentElement.style.setProperty('--primary-light', light);
        document.documentElement.style.setProperty('--teamtodo-primary-rgb', `${rgb.r} ${rgb.g} ${rgb.b}`);
        ensureThemeOverrideStyle(normalized, light, rgb);

        if (window.AndroidBridge) {
            nativeCall('setThemeColor', { color: normalized });
        }
    }

    // =============================
    // 濡쒕뵫 ?뚮━而?諛⑹?
    // =============================
    function preventFlicker() {
        document.documentElement.style.visibility = 'hidden';
        document.addEventListener('DOMContentLoaded', () => {
            document.documentElement.style.visibility = '';
        });
    }

    function setupIconFontGuard() {
        const docEl = document.documentElement;
        if (docEl.dataset.iconGuardInit === '1') return;
        docEl.dataset.iconGuardInit = '1';

        const styleId = 'teamtodo-icon-font-guard';
        if (!document.getElementById(styleId)) {
            const style = document.createElement('style');
            style.id = styleId;
            style.textContent = `
                html:not(.fonts-ready) .material-symbols-outlined,
                html:not(.fonts-ready) .material-icons {
                    visibility: hidden;
                }
            `;
            document.head.appendChild(style);
        }

        const reveal = () => docEl.classList.add('fonts-ready');
        const fallback = setTimeout(reveal, 1200);

        if (document.fonts && document.fonts.ready) {
            document.fonts.ready.then(() => {
                clearTimeout(fallback);
                reveal();
            }).catch(() => {
                clearTimeout(fallback);
                reveal();
            });
        } else {
            reveal();
        }
    }

    function resolveBottomNavScreen(item) {
        if (!item) return null;

        const explicit = item.getAttribute('data-nav-screen');
        if (explicit) return explicit;

        const idMap = {
            navTodo: 'todo_main',
            navCalendar: 'calendar',
            navTeam: 'team_list',
            navGoal: 'goal_list',
            navSettings: 'settings'
        };
        if (item.id && idMap[item.id]) return idMap[item.id];

        const iconEl = item.querySelector('.material-symbols-outlined, .material-icons');
        const iconName = iconEl ? (iconEl.textContent || '').replace(/\s+/g, '').toLowerCase() : '';
        if (iconName) {
            if (iconName.includes('calendar')) return 'calendar';
            if (iconName.includes('group') || iconName.includes('people')) return 'team_list';
            if (iconName.includes('flag')) return 'goal_list';
            if (iconName.includes('setting')) return 'settings';
            if (iconName.includes('check') || iconName.includes('task')) return 'todo_main';
        }

        return null;
    }

    function setupGlobalBottomNav() {
        const rootSelector = '#bottomNav, #bottomNavigation, .bottom-nav, nav.fixed.bottom-0, nav[class*="bottom-nav"]';
        document.addEventListener('click', function (e) {
            const target = e.target;
            if (!target || typeof target.closest !== 'function') return;

            const navRoot = target.closest(rootSelector);
            if (!navRoot) return;

            const item = target.closest('[data-nav-screen], a, button, .nav-item, [id^="nav"]');
            if (!item || !navRoot.contains(item)) return;

            // These IDs are already wired in page scripts.
            if (/^nav(Todo|Calendar|Team|Goal|Settings)$/.test(item.id || '')) return;

            const inlineOnclick = item.getAttribute('onclick') || '';
            if (inlineOnclick.includes('TeamTodo.navigate(')) return;

            const screen = resolveBottomNavScreen(item);
            if (!screen) return;

            if (item.tagName === 'A') {
                const href = (item.getAttribute('href') || '').trim();
                if (!href || href === '#') e.preventDefault();
            }
            TeamTodo.navigate(screen);
        });
    }

    // =============================
    // Public API
    // =============================
    window.TeamTodo = {
        // 濡쒖슦?덈꺼 釉뚮━吏 ?몄텧
        call: (method, data, cb) => nativeCall(method, data, cb),

        // 네비게이션
        navigate: (screen) => nativeCall('navigate', { screen }),

        // ?몄쬆
        login: () => nativeCall('googleLogin', {}),
        logout: () => nativeCall('logout', {}),
        getCurrentUser: (cb) => nativeCall('getCurrentUser', {}, cb),

        // 프로필
        getProfiles: (cb) => nativeCall('getProfiles', {}, cb),
        saveProfile: (data, cb) => nativeCall('saveProfile', data, cb),
        deleteProfile: (id, cb) => nativeCall('deleteProfile', { id }, cb),
        setCurrentProfile: (profile) => { state.currentProfile = profile; },
        getCurrentProfile: () => state.currentProfile,

        // 할일
        getTodos: (profileId, date, cb) => nativeCall('getTodos', { profileId, date }, cb),
        saveTodo: (data, cb) => nativeCall('saveTodo', data, cb),
        deleteTodo: (id, cb) => nativeCall('deleteTodo', { id }, cb),
        completeTodo: (id, completed, cb) => nativeCall('completeTodo', { id, completed }, cb),

        // ?곕큺
        addThumbsUp: (todoId, receiverProfileId, comment, cb, receiverUid) =>
            nativeCall('addThumbsUp', { todoId, receiverProfileId, comment, receiverUid: receiverUid || '' }, cb),
        removeThumbsUp: (todoId, cb) => nativeCall('removeThumbsUp', { todoId }, cb),
        getThumbsUps: (todoId, cb) => nativeCall('getThumbsUps', { todoId }, cb),

        // ?
        getTeams: (cb) => nativeCall('getTeams', {}, cb),
        joinTeam: (teamId, cb) => nativeCall('joinTeam', { teamId }, cb),
        leaveTeam: (teamId, cb) => nativeCall('leaveTeam', { teamId }, cb),

        // 紐⑺몴
        getGoals: (profileId, cb) => nativeCall('getGoals', { profileId }, cb),
        saveGoal: (data, cb) => nativeCall('saveGoal', data, cb),
        deleteGoal: (id, cb) => nativeCall('deleteGoal', { id }, cb),

        // ?뚮엺
        setAlarm: (id, title, timeMillis, isMorningCall) =>
            nativeCall('setAlarm', { id, title, timeMillis, isMorningCall }),
        cancelAlarm: (id) => nativeCall('cancelAlarm', { id }),

        // 포인트
        getPoints: (profileId, cb) => nativeCall('getPoints', { profileId }, cb),

        // 紐낆뼵
        getDailyQuote: (cb) => nativeCall('getDailyQuote', {}, cb),

        // 留곹겕
        saveLink: (todoId, url, title, cb) => nativeCall('saveLink', { todoId, url, title }, cb),
        deleteLink: (todoId, url, cb) => nativeCall('deleteLink', { todoId, url }, cb),
        seedSampleData: (cb) => nativeCall('seedSampleData', {}, cb),
        resetMyData: (cb) => nativeCall('resetMyData', {}, cb),

        // ?뚮쭏
        applyTheme,

        // 애니메이션
        animateHeartToPointBar,
        triggerConfetti,

        // ?곹깭
        state
    };

    // 釉뚮┸吏 以鍮??꾨즺
    window.onBridgeReady = function () {
        console.log('[TeamTodo] Bridge ready');
        const event = new Event('teamtodo:ready');
        document.dispatchEvent(event);
    };

    // DOM ready initialization
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

    function init() {
        setupIconFontGuard();

        // ??λ맂 ?뚮쭏 ?됱긽 ?곸슜
        const savedColor = localStorage.getItem('themeColor') || '#FF6B9D';
        applyTheme(savedColor);
        const darkOn = localStorage.getItem('darkMode') === 'true';
        if (darkOn) document.documentElement.classList.add('dark');
        else document.documentElement.classList.remove('dark');
        setupGlobalBottomNav();

        // ?꾩옱 ?ъ슜??議고쉶
        if (window.AndroidBridge) {
            TeamTodo.getCurrentUser(() => { });
        }

        const reveal = () => {
            document.documentElement.classList.remove('tt-preparing');
            document.documentElement.classList.add('tt-ready');
            setTimeout(() => {
                document.documentElement.classList.remove('tt-no-transitions');
            }, 120);
        };
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => requestAnimationFrame(reveal), { once: true });
        } else {
            requestAnimationFrame(reveal);
        }
        setTimeout(reveal, 450);
    }

})();

