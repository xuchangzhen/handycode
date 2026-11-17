import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {runInNewContext} from 'node:vm';
const source=readFileSync(new URL('../app/src/main/assets/station-capture.js',import.meta.url),'utf8');
function setup(url='https://pages-fast.m.taobao.com/wow/z/uniapp/1100333/last-mile-fe/m-end-school-tab/home'){
 function Xhr(){this.listeners=[];this.responseURL='https://api.m.taobao.com/fixture';this.responseType='';}
 Xhr.prototype.send=function(){return 7};Xhr.prototype.addEventListener=function(_,fn){this.listeners.push(fn)};
 const window={};const ctx={window,location:new URL(url),URL,XMLHttpRequest:Xhr};runInNewContext(source,ctx);return {ctx,Xhr};
}
let f=setup();let x=new f.Xhr();assert.equal(x.send(),7);
x.responseText=JSON.stringify({data:{items:[{mailNo:'FAKEWAYBILL1',takeCode:'FAKE-1',statusName:'待取件',stationName:'虚构驿站',cpName:'虚构物流'},
 {mailNo:'FAKEWAYBILL2',takeCode:'FAKE-1',statusName:'待领取'},{mailNo:'FAKEWAYBILL3'},{mailNo:'FAKEWAYBILL4',takeCode:'FAKE-4',statusName:'已取件'}]}});
x.listeners.forEach(fn=>fn.call(x));let rows=f.ctx.window.HandyStationSnapshot.snapshot().rows;
assert.equal(rows.length,2);assert.ok(rows.some(row=>row.tracking==='FAKEWAYBILL1'&&row.station==='虚构驿站'));
let foreign=new f.Xhr();foreign.responseURL='https://taobao.com.evil.invalid/';foreign.send();foreign.responseText='{"mailNo":"FAKEWAYBILL4","takeCode":"FAKE-4"}';foreign.listeners.forEach(fn=>fn.call(foreign));
assert.equal(f.ctx.window.HandyStationSnapshot.snapshot().rows.length,2);
assert.equal(setup('https://pages-fast.m.taobao.com/login').ctx.window.HandyStationSnapshot,undefined);
assert.equal(setup('http://pages-fast.m.taobao.com/wow/z/uniapp/1100333/last-mile-fe/m-end-school-tab/home').ctx.window.HandyStationSnapshot,undefined);
console.log('PASS: official station response scope, stable identifiers and duplicate pickup codes');

const pdd=setup('https://m.pinduoduo.net/mdkd/package');const px=new pdd.Xhr();px.responseURL='https://m.pinduoduo.net/proxy/api/example';px.send();
px.responseText=JSON.stringify({tracking_number:'FAKEWAYBILL9',pickup_code:'F-999',status_desc:'待取件'});px.listeners.forEach(fn=>fn.call(px));
assert.equal(pdd.ctx.window.HandyStationSnapshot.snapshot().rows[0].tracking,'FAKEWAYBILL9');
