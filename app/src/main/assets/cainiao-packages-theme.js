;(function () {
 'use strict';
 if (window.HandyCainiaoPackagesTheme) return;
 var css = `
html[data-hc-cainiao-packages],html[data-hc-cainiao-packages] body {background:#f7f8fb!important;color:#242b37!important}
html[data-hc-cainiao-packages] :is(#wrapper,.iscroll,.topbar) {background:transparent!important}
html[data-hc-cainiao-packages] .topbar {padding:12px 10px 0!important}
html[data-hc-cainiao-packages] .search {box-sizing:border-box;background:#fff!important;border:1px solid #e9ecf0!important;border-radius:14px!important}
html[data-hc-cainiao-packages] .banner {display:none!important}
html[data-hc-cainiao-packages] .express-list {box-sizing:border-box;width:auto!important;margin:12px 10px!important;padding:0!important;background:#fff!important;border:1px solid #e9ecf0!important;border-radius:16px!important;overflow:hidden}
html[data-hc-cainiao-packages] .list-title {box-sizing:border-box;display:flex;align-items:center;min-height:53px;padding:12px 14px!important;border-bottom:1px solid #f0f2f5;color:#242b37!important;font-size:16px!important;font-weight:600!important}
html[data-hc-cainiao-packages] .list-item {box-sizing:border-box;width:100%!important;margin:0!important;padding:12px 14px!important;background:transparent!important;border:0!important;border-bottom:1px solid #f0f2f5!important;border-radius:0!important}
html[data-hc-cainiao-packages] .list-item:last-child {border-bottom:0!important}
html[data-hc-cainiao-packages] .list-item::before {border:0!important;pointer-events:none}
html[data-hc-cainiao-packages] .express-info_header {padding:0 0 8px!important}
html[data-hc-cainiao-packages] .express-info_title {color:#242b37!important;font-size:13px!important;font-weight:600!important;line-height:1.4!important}
html[data-hc-cainiao-packages] .express-info_body {display:flex!important;align-items:center!important;margin:0!important;padding:0!important}
html[data-hc-cainiao-packages] .express-info_thumb {box-sizing:border-box;flex:0 0 44px!important;width:44px!important;min-width:44px!important;margin:0 10px 0 0!important;padding:0!important}
html[data-hc-cainiao-packages] .express-info_thumb img {box-sizing:border-box;width:44px!important;height:44px!important;object-fit:cover;border:0!important;border-radius:8px!important}
html[data-hc-cainiao-packages] .express-info_detail {box-sizing:border-box;min-width:0!important;padding:0!important;color:#697386!important;font-size:12px!important;line-height:1.4!important}
html[data-hc-cainiao-packages] .express-info_detail > div {margin:0 0 3px!important;color:#697386!important;overflow-wrap:anywhere}
html[data-hc-cainiao-packages] .express-info_detail > div:last-child {margin-bottom:0!important;color:#bd451f!important}
html[data-hc-cainiao-packages] :is(.express-list_empty,.brand_footer) {color:#697386!important}
html[data-hc-cainiao-packages] .search input {background:transparent!important;color:#242b37!important}
html[data-hc-cainiao-packages="dark"],html[data-hc-cainiao-packages="dark"] body {background:#0f1218!important;color:#eceff4!important;color-scheme:dark}
html[data-hc-cainiao-packages="dark"] :is(.search,.express-list) {background:#181c24!important;border-color:#262c36!important}
html[data-hc-cainiao-packages="dark"] :is(.list-title,.list-item) {border-color:#232833!important}
html[data-hc-cainiao-packages="dark"] :is(.list-title,.express-info_title,.express-info_detail,.search input) {color:#eceff4!important}
html[data-hc-cainiao-packages="dark"] .express-info_detail > div {color:#b3bdcc!important}
html[data-hc-cainiao-packages="dark"] .express-info_detail > div:last-child {color:#ff8e6b!important}
html[data-hc-cainiao-packages="dark"] :is(.express-list_empty,.brand_footer) {color:#949eb0!important}
`;
 function update(dark) {
  var root=document.documentElement;
  if (!root) return;
  if (location.protocol !== 'https:' || location.hostname !== 'page.cainiao.com'
      || location.pathname !== '/guoguo/app-myexpress-taobao/') {
   root.removeAttribute('data-hc-cainiao-packages'); return;
  }
  var style=document.getElementById('hc-cainiao-packages-theme');
  if (!style) { style=document.createElement('style');style.id='hc-cainiao-packages-theme';style.textContent=css;(document.head||root).appendChild(style); }
  root.setAttribute('data-hc-cainiao-packages',dark?'dark':'light');
 }
 window.HandyCainiaoPackagesTheme={update:update};
})();
