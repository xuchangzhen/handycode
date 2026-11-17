(function () {
 'use strict';
 if (window.HandyCodePhoneTheme) return;
 var dark = false;
 var css = `
html[data-hc-phone-theme="dark"] {
 color-scheme:dark;
 background:#0f1218!important;
 color:#eceff4!important;
 --end-color-text:#eceff4;
 --end-color-text-secondary:#b3bdcc;
 --end-color-text-weak:#949eb0;
 --end-color-fill:#949eb0;
 --end-color-border:#303744;
 --bg-primary:#232936;
}
html[data-hc-phone-theme="dark"] body {background:#0f1218!important;color:#eceff4!important}
html[data-hc-phone-provider="taobao"][data-hc-phone-theme="dark"] #ice-container {color:#eceff4}
html[data-hc-phone-provider="taobao"][data-hc-phone-theme="dark"] :is([class^="phone-manage--"],[class^="add-friend-phone--"],[class^="card--"],[class^="button-group--"]) {background-color:#181c24!important}
html[data-hc-phone-provider="taobao"][data-hc-phone-theme="dark"] :is([class^="card--"] [class^="title--"],[class^="phone--"],[class^="card-title--"],[class^="label--"]) {color:#eceff4!important}
html[data-hc-phone-provider="taobao"][data-hc-phone-theme="dark"] :is([class^="content--"],[class^="edit--"],[class^="delete--"],[class^="footer-info--"]) {color:#b3bdcc!important}
html[data-hc-phone-provider="taobao"][data-hc-phone-theme="dark"] :is([class^="input-wrap--"],[class^="phone-item--"] [class^="info--"]) {background:#232936!important;color:#b3bdcc!important}
html[data-hc-phone-provider="taobao"][data-hc-phone-theme="dark"] :is([class^="container--hIneu"],[class^="fixed-bottom--"]) {background:#181c24!important}
html[data-hc-phone-provider="taobao"][data-hc-phone-theme="dark"] [class^="container--hIneu"] img {opacity:.18}
html[data-hc-phone-provider="taobao"][data-hc-phone-theme="dark"] [class^="header-concent--"] {color:#eceff4!important}
html[data-hc-phone-provider="taobao"][data-hc-phone-theme="dark"] :is([class^="header-grey--"],[class^="tip--"]) {color:#b3bdcc!important}
html[data-hc-phone-provider="taobao"][data-hc-phone-theme="dark"] input {background:transparent!important;color:#eceff4!important;caret-color:#ff8e6b;--end-input-color:#eceff4;--end-input-placeholder-color:#949eb0}
html[data-hc-phone-provider="taobao"][data-hc-phone-theme="dark"] input::placeholder {color:#949eb0!important;opacity:1}
html[data-hc-phone-provider="taobao"][data-hc-phone-theme="dark"] [class^="card--"] [class^="text--"] {color:#b3bdcc!important}
html[data-hc-phone-provider="taobao"][data-hc-phone-theme="dark"] :is([class^="button-group--"] .hyper-button-color-primary-fill-solid,[class^="label-required--"]) {--primary-color:#ff784e}

html[data-hc-package-form="taobao"] [data-hc-package-extra] {display:none!important}
html[data-hc-package-form="taobao"][data-hc-phone-theme="dark"] :is([class^="header--"],.hyper-popup-content,.hyper-center-popup-content,[class^="modal--"],[class^="bind-modal--"],[class^="verify-wrap--"]) {background:#181c24!important;color:#eceff4!important}
html[data-hc-package-form="taobao"][data-hc-phone-theme="dark"] :is([class^="input--"],.end-input,[class^="verify-container--"],[class^="tel--"],[class^="item--"]) {background:#232936!important;color:#eceff4!important}
html[data-hc-package-form="taobao"][data-hc-phone-theme="dark"] :is([class^="title--"],[class^="concent-title--"],[class^="concent-desc--"],[class^="header-wrap--"],[class^="phone--"],[class^="tel-label--"]) {color:#eceff4!important}
html[data-hc-package-form="taobao"][data-hc-phone-theme="dark"] :is([class^="desc--"],[class^="concent-info--"],[class^="content-wrap--"],[class^="info--"],[class^="timeout--"]) {color:#b3bdcc!important}
html[data-hc-package-form="taobao"][data-hc-phone-theme="dark"] :is([class^="cancel-btn--"],.hyper-button-color-default-fill-solid) {background:#232936!important;color:#eceff4!important;border-color:#465164!important}
html[data-hc-package-form="taobao"][data-hc-phone-theme="dark"] :is(input,textarea) {color:#eceff4!important;caret-color:#ff8e6b}

html[data-hc-phone-provider="pdd"][data-hc-phone-theme="dark"] [class*="cell-phone-list-container__"] {background:#0f1218!important}
html[data-hc-phone-provider="pdd"][data-hc-phone-theme="dark"] :is(.cell-phone-list-info,.cell-phone-list-item,.cell-phone-button-wrapper,.event-modal-view-container-center,.event-modal-content,.bind-new-cell-phone-wrapper,.modify-cell-phone-wrapper) {background:#181c24!important;color:#eceff4!important}
html[data-hc-phone-provider="pdd"][data-hc-phone-theme="dark"] :is(.cell-phone-list-title,.cell-phone-list-item-label-value,.cell-phone-list-item-actions,.bind-new-cell-phone-content-title,.modify-cell-phone-content-title) {color:#eceff4!important}
html[data-hc-phone-provider="pdd"][data-hc-phone-theme="dark"] :is(.cell-phone-list-item-actions .default-container,.cell-phone-list-item-actions .selected) {color:#eceff4!important}
html[data-hc-phone-provider="pdd"][data-hc-phone-theme="dark"] :is(.cell-phone-list-desc,.bind-new-cell-phone-content-desc,.bind-new-contract-wrapper,.bind-new-contract-item-fill,.modify-cell-phone-content-desc) {color:#949eb0!important}
html[data-hc-phone-provider="pdd"][data-hc-phone-theme="dark"] :is(.cell-phone-list-item-operate span,.modify-cell-phone-content-alias-item) {background:#232936!important;color:#eceff4!important;border-color:#465164!important}
html[data-hc-phone-provider="pdd"][data-hc-phone-theme="dark"] :is(.cell-phone-list-item-divider,.bind-new-cell-phone-content-item,.bind-new-cell-phone-content-tips,.modify-cell-phone-content-item) {border-color:#303744!important}
html[data-hc-phone-provider="pdd"][data-hc-phone-theme="dark"] .cell-phone-list-item-divider {background:#303744!important}
html[data-hc-phone-provider="pdd"][data-hc-phone-theme="dark"] :is(.bind-new-cell-phone-content-item,.modify-cell-phone-content-item) input {background:transparent!important;color:#eceff4!important;caret-color:#ff8e6b}
html[data-hc-phone-provider="pdd"][data-hc-phone-theme="dark"] input::placeholder {color:#949eb0!important;opacity:1}
html[data-hc-phone-provider="pdd"][data-hc-phone-theme="dark"] .cell-phone-button-tip {background:#3c3022!important;color:#ffd19a!important}
html[data-hc-phone-provider="pdd"][data-hc-phone-theme="dark"] .bind-new-contract-item-content {color:#79b9e8!important}

/* Keep the official inputs, buttons and verification flow; only restyle their surfaces. */
html[data-hc-phone-provider="pdd"][data-hc-phone-theme="light"] body {background:#f5f6f8!important;color:#242b37!important}
html[data-hc-phone-provider="pdd"] [class*="cell-phone-list-container__"] {min-height:100vh;background:#f5f6f8}
html[data-hc-phone-provider="pdd"] :is(.cell-phone-list-info,.cell-phone-list-item,.bind-new-cell-phone-wrapper,.modify-cell-phone-wrapper) {border-radius:16px}
html[data-hc-phone-provider="pdd"] :is(.cell-phone-list-info,.cell-phone-list-item) {overflow:hidden}
html[data-hc-phone-provider="pdd"] :is(.bind-new-cell-phone-content-item,.modify-cell-phone-content-item) {border-radius:10px}
html[data-hc-package-form="pdd"] [class*="add-package-package__"] {box-sizing:border-box;min-height:100vh;background:#f5f6f8;color:#242b37}
html[data-hc-package-form="pdd"] :is(.selected-station-container,.text-area) {box-sizing:border-box;background:#fff;border:1px solid #e9ecf0;border-radius:16px}
html[data-hc-package-form="pdd"] :is(.selected-station-container,.text-area,textarea,input) {transition:background-color .2s,border-color .2s,color .2s}
html[data-hc-package-form="pdd"] :is(textarea,input) {caret-color:#e56c43}
html[data-hc-package-form="pdd"] textarea::placeholder {color:#929baa;opacity:1}
/* The official add-package page nests the textarea and submit action inside
   .text-area-wrap. Keep those nodes and handlers, but lay them out as a card. */
html[data-hc-package-form="pdd"] {--hc-form-surface:#fff;--hc-form-field:#f7f8fb;--hc-form-border:#e9ecf0;--hc-form-ink:#242b37;--hc-form-muted:#697386}
html[data-hc-package-form="pdd"][data-hc-phone-theme="dark"] {--hc-form-surface:#181c24;--hc-form-field:#232936;--hc-form-border:#303744;--hc-form-ink:#eceff4;--hc-form-muted:#a9b3c2}
html[data-hc-package-form="pdd"] [class*="add-package-package__"] {padding:16px 12px 28px!important}
html[data-hc-package-form="pdd"] .text-area-wrap {box-sizing:border-box!important;position:relative!important;display:flex!important;flex-direction:column!important;gap:12px!important;height:auto!important;min-height:0!important;margin:0!important;padding:18px 14px 14px!important;background:var(--hc-form-surface)!important;border:1px solid var(--hc-form-border)!important;border-radius:16px!important;animation:hc-form-enter .25s ease-out}
html[data-hc-package-form="pdd"] .text-area-wrap::before {content:'填写运单号';display:block;color:var(--hc-form-ink);font:600 16px/1.4 system-ui,sans-serif}
html[data-hc-package-form="pdd"] .text-area-wrap::after {content:'请输入完整运单号，查询结果以平台为准';display:block;color:var(--hc-form-muted);font:12px/1.5 system-ui,sans-serif}
html[data-hc-package-form="pdd"] .text-area-wrap > div:not(.submit-btn) {width:100%!important;min-width:0!important;height:auto!important}
html[data-hc-package-form="pdd"] .text-area-wrap .text-area {box-sizing:border-box!important;width:100%!important;min-height:112px!important;height:auto!important;margin:0!important;padding:12px!important;background:var(--hc-form-field)!important;border:1px solid var(--hc-form-border)!important;border-radius:12px!important}
html[data-hc-package-form="pdd"] .text-area-wrap .rm-textarea-control {width:100%!important;min-width:0!important}
html[data-hc-package-form="pdd"] .text-area-wrap textarea {box-sizing:border-box!important;width:100%!important;min-height:86px!important;height:86px!important;margin:0!important;padding:0!important;border:0!important;outline:0!important;background:transparent!important;color:var(--hc-form-ink)!important;font:14px/1.5 system-ui,sans-serif!important;resize:none}
html[data-hc-package-form="pdd"] .text-area-wrap .submit-btn {box-sizing:border-box!important;position:static!important;display:flex!important;align-items:center!important;justify-content:center!important;width:100%!important;height:44px!important;min-height:44px!important;margin:0!important;border:0!important;border-radius:12px!important;background:#eb6238!important;color:#fff!important;font:600 14px/1 system-ui,sans-serif!important}
html[data-hc-package-form="pdd"] .text-area-wrap .submit-btn:active {opacity:.8}
html[data-hc-phone-provider="pdd"].hc-pdd-source-mode #hc-pdd-back {display:none!important}
html[data-hc-phone-provider="pdd"].hc-pdd-source-mode [data-hc-pdd-source] {top:0!important;height:100vh!important}
@keyframes hc-form-enter {from{opacity:0;transform:translateY(8px)}to{opacity:1;transform:translateY(0)}}
@media (prefers-reduced-motion:reduce) {html[data-hc-package-form="pdd"] :is(.selected-station-container,.text-area,textarea,input) {transition:none}}
@media (prefers-reduced-motion:reduce) {html[data-hc-package-form="pdd"] .text-area-wrap {animation:none!important}}
html[data-hc-package-form="pdd"] :is(.history-phone-list-wrap,.strategy-list-container,.notice) {display:none!important}
html[data-hc-package-form="pdd"][data-hc-phone-theme="dark"] [class*="add-package-package__"] {background:#0f1218!important;color:#eceff4!important;min-height:100vh}
html[data-hc-package-form="pdd"][data-hc-phone-theme="dark"] :is(.selected-station-container,.text-area,.event-modal-content,.event-modal-view-container-center,.query-waybill-modal-container,.rm-modal-content) {background:#181c24!important;color:#eceff4!important;border-color:#303744!important}
html[data-hc-package-form="pdd"][data-hc-phone-theme="dark"] .text-area-wrap .text-area {background:var(--hc-form-field)!important}
html[data-hc-package-form="pdd"][data-hc-phone-theme="dark"] :is(.name,.action,.query-waybill-title,[class*="station-name"],.event-modal-title) {color:#eceff4!important}
html[data-hc-package-form="pdd"][data-hc-phone-theme="dark"] :is(.address,.event-modal-desc,.event-modal-content-text) {color:#b3bdcc!important}
html[data-hc-package-form="pdd"][data-hc-phone-theme="dark"] :is(textarea,input) {background:transparent!important;color:#eceff4!important;caret-color:#ff8e6b}
html[data-hc-package-form="pdd"][data-hc-phone-theme="dark"] textarea::placeholder {color:#949eb0!important;opacity:1}
`;
 var packageObserver;
 function simplifyPackageSearch() {
  if (document.documentElement.getAttribute('data-hc-package-form') === 'pdd') {
   var input = document.querySelector('[class*="add-package-package__"] textarea');
   if (input) {
    if (input.placeholder !== '请输入完整运单号') input.setAttribute('placeholder', '请输入完整运单号');
    if (input.getAttribute('aria-label') !== '运单号') input.setAttribute('aria-label', '运单号');
   }
   return;
  }
  var container = document.querySelector('[class^="school-search--"]');
  if (!container) return;
  var file = container.querySelector('input[type="file"]');
  if (file) {
   var branch = file;
   while (branch.parentElement && branch.parentElement !== container) branch = branch.parentElement;
   if (branch.parentElement === container && !branch.hasAttribute('data-hc-package-extra')) branch.setAttribute('data-hc-package-extra', '');
  }
  container.querySelectorAll('[class*="history-pkg--"],[class*="rowFeeds--"]').forEach(function (extra) {
   if (!extra.hasAttribute('data-hc-package-extra')) extra.setAttribute('data-hc-package-extra', '');
  });
 }
 function setDark(value) {
  dark = !!value;
  var root = document.documentElement;
  if (!root) return;
  var provider = '';
  if (location.hostname === 'm.pinduoduo.net' && /^\/mdkd\/(?:cellPhoneList|add-package)$/.test(location.pathname)) provider = 'pdd';
  if (location.hostname === 'pages-fast.m.taobao.com'
      && /^\/wow\/z\/uniapp\/1011717\/last-mile-fe\/end-collect-platform\/(?:phone-manage(?:-xy)?(?:-ssr)?|add-friend-phone(?:-ssr)?|school-search(?:-ssr)?)$/.test(location.pathname)) provider = 'taobao';
  if (!provider) {
   root.removeAttribute('data-hc-phone-provider');
   root.removeAttribute('data-hc-phone-theme');
   root.removeAttribute('data-hc-package-form');
   if (packageObserver) { packageObserver.disconnect(); packageObserver = null; }
   return;
  }
  var style = document.getElementById('hc-phone-theme');
  if (!style) {
   style = document.createElement('style'); style.id = 'hc-phone-theme'; style.textContent = css;
   (document.head || root).appendChild(style);
  }
  root.setAttribute('data-hc-phone-provider', provider);
  root.setAttribute('data-hc-phone-theme', dark ? 'dark' : 'light');
  if (provider === 'taobao' && /\/school-search(?:-ssr)?$/.test(location.pathname)
      || provider === 'pdd' && location.pathname === '/mdkd/add-package') {
   root.setAttribute('data-hc-package-form', provider);
   simplifyPackageSearch();
   if (!packageObserver) {
    packageObserver = new MutationObserver(simplifyPackageSearch);
    packageObserver.observe(root, {subtree:true,childList:true});
   }
  } else {
   root.removeAttribute('data-hc-package-form');
   if (packageObserver) { packageObserver.disconnect(); packageObserver = null; }
  }
 }
 window.HandyCodePhoneTheme = {setDark:setDark};
 window.addEventListener('popstate', function () { setDark(dark); });
})();
