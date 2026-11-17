import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {runInNewContext} from 'node:vm';
import {parseHTML} from 'linkedom';
const script=readFileSync(new URL('../app/src/main/assets/cainiao-packages-theme.js',import.meta.url),'utf8');
function fixture(url) {
 const {window,document}=parseHTML('<html><head></head><body><div id="wrapper"><div class="express-list"><div class="list-title">待收包裹</div><div class="list-item"><div class="express-info_detail">测试内容</div><button>删除</button></div></div></div></body></html>');
 delete window.HandyCainiaoPackagesTheme;
 const context={window,document,location:new URL(url)};runInNewContext(script,context);return {window,document,context};
}
const f=fixture('https://page.cainiao.com/guoguo/app-myexpress-taobao/');
const item=f.document.querySelector('.list-item'),button=f.document.querySelector('button');let clicks=0;button.addEventListener('click',()=>clicks++);
f.window.HandyCainiaoPackagesTheme.update(true);assert.equal(f.document.documentElement.dataset.hcCainiaoPackages,'dark');
f.window.HandyCainiaoPackagesTheme.update(false);assert.equal(f.document.documentElement.dataset.hcCainiaoPackages,'light');
runInNewContext(script,f.context);assert.equal(f.document.querySelectorAll('#hc-cainiao-packages-theme').length,1);
const css=f.document.getElementById('hc-cainiao-packages-theme').textContent;
assert.match(css,/\.express-list \{[^}]*border-radius:16px/,'The visible official rows share a rounded card');
assert.match(css,/\.express-info_thumb img \{[^}]*width:44px/,'Parcel images use the compact layout');
assert.doesNotMatch(css,/\.express-info_more\s*\{[^}]*display\s*:\s*none/,'Official row actions are not hidden');
assert.equal(f.document.querySelector('.list-item'),item);assert.equal(f.document.querySelector('button'),button);button.click();assert.equal(clicks,1);
for (const url of ['http://page.cainiao.com/guoguo/app-myexpress-taobao/','https://page.cainiao.com.evil.invalid/guoguo/app-myexpress-taobao/','https://page.cainiao.com/other']) {const x=fixture(url);x.window.HandyCainiaoPackagesTheme.update(true);assert.equal(x.document.documentElement.hasAttribute('data-hc-cainiao-packages'),false);assert.equal(x.document.getElementById('hc-cainiao-packages-theme'),null)}
console.log('PASS: scoped Cainiao package theme, original nodes and handlers, reversible light/dark');
