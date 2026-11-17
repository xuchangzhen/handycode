import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {runInNewContext} from 'node:vm';
import {parseHTML} from 'linkedom';
const source=readFileSync(new URL('../app/src/main/assets/pickup-snapshot.js',import.meta.url),'utf8');
function run(url,html,subframe=false){
 const {window,document}=parseHTML(`<html>${html}</html>`);window.top=subframe?{}:window;
 return runInNewContext(source,{window,document,location:new URL(url)});
}
const pdd='<body><div id="hc-pdd-identity"><span class="package-code-value">IDENTITY-DO-NOT-READ</span></div><div id="hc-pdd-parcels" data-ready="true"><span class="package-code-value">FAKE-123</span><span class="package-code-value">FAKE-123</span><input value="SECRET"></div></body>';
let r=run('https://m.pinduoduo.net/mdkd/package',pdd);
assert.equal(r.rows.length,2,'Never deduplicate by pickup code');assert.equal(r.rows[0].code,'FAKE-123');
assert.equal(run('https://evil.invalid/mdkd/package',pdd),null);
assert.equal(run('https://m.pinduoduo.net/mdkd/package',pdd,true),null);
assert.equal(run('https://m.pinduoduo.net/mdkd/package',pdd.replace('data-ready="true"','')).ready,false);
const tb='<body id="handycode-merged"><div id="identity-content">取件码 IDENTITY-SECRET</div><div id="parcels-content" data-ready="true"><p>取件码：FAKE-456</p></div></body>';
r=run('https://pages-fast.m.taobao.com/wow/z/uniapp/1011717/last-mile-fe/end-collect-platform/identity-code',tb);
assert.equal(r.rows.length,1);assert.equal(r.rows[0].code,'FAKE-456');
assert.equal(run('http://pages-fast.m.taobao.com/wow/z/uniapp/1011717/last-mile-fe/end-collect-platform/identity-code',tb),null);
console.log('PASS: bounded pickup projection, origin, readiness and identity isolation');
