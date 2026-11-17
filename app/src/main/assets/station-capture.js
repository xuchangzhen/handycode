(function(){
 'use strict';
 // Installed only in the official Taobao station document; do not touch identity or login pages.
 var taobao=location.hostname==='pages-fast.m.taobao.com' && location.pathname==='/wow/z/uniapp/1100333/last-mile-fe/m-end-school-tab/home';
 var pdd=location.hostname==='m.pinduoduo.net' && location.pathname==='/mdkd/package';
 if(location.protocol!=='https:' || (!taobao&&!pdd) || window.HandyStationSnapshot)return;
 var rows=[],revision=0,signature='',token=Date.now().toString(36)+'-'+Math.random().toString(36).slice(2);
 function official(raw){
  try {var u=new URL(raw,location.href),h=u.hostname.toLowerCase();
   return u.protocol==='https:' && !u.username && !u.password && (!u.port || u.port==='443') && (taobao
    ? (h==='taobao.com'||h.endsWith('.taobao.com')||h==='cainiao.com'||h.endsWith('.cainiao.com'))
    : (h==='pinduoduo.net'||h.endsWith('.pinduoduo.net')||h==='pinduoduo.com'||h.endsWith('.pinduoduo.com')));
  }catch(e){return false;}
 }
 function value(o,keys,max){
  for(var i=0;i<keys.length;i++)if(typeof o[keys[i]]==='string' && o[keys[i]].trim())return o[keys[i]].trim().slice(0,max);
  return '';
 }
 function consume(text){
  if(typeof text!=='string'||text.length>2097152)return;
  var raw=text.trim(), match=raw.match(/^[A-Za-z_$][\w.$]*\s*\(([\s\S]*)\)\s*;?$/);
  if(match)raw=match[1];
  var data;try{data=JSON.parse(raw);}catch(e){return;}
  var stack=[data],visited=0,found=[];
  while(stack.length && visited++<3000 && found.length<100){
   var item=stack.pop();if(!item||typeof item!=='object')continue;
   if(Array.isArray(item)){for(var i=0;i<item.length&&i<500;i++)stack.push(item[i]);continue;}
   var tracking=value(item,['mailNo','waybillNo','trackingNo','trackingNumber','tracking_number','mail_no'],100);
   var code=value(item,['takeCode','fetchCode','pickupCode','shelfCode','pickup_code','take_code'],32);
   var status=value(item,['statusName','statusDesc','statusText','status_name','status_desc'],100);
   var pending=/待取件|待领取|等待取件|等待领取|驿站代签|代收点代签/.test(status)
    && !/已取件|已领取|取消|关闭|退款|拒收|无需取件/.test(status);
   if(pending && /^[A-Za-z0-9-]{8,100}$/.test(tracking)&&/^[A-Za-z0-9-]{3,32}$/.test(code)){
    found.push({tracking:tracking,code:code,status:status,carrier:value(item,['cpName','expressCompanyName'],100),station:value(item,['stationName','siteName','stationDesc'],180)});
   }
   Object.keys(item).slice(0,200).forEach(function(k){if(item[k]&&typeof item[k]==='object')stack.push(item[k]);});
  }
  if(found.length){var next=JSON.stringify(found);if(next!==signature){signature=next;rows=found;revision++;}}
 }
 var send=XMLHttpRequest.prototype.send;
 XMLHttpRequest.prototype.send=function(){
  this.addEventListener('load',function(){try{
   if(!official(this.responseURL))return;
   if(this.responseType==='json')consume(JSON.stringify(this.response));
   else if(!this.responseType||this.responseType==='text')consume(this.responseText);
  }catch(e){}});
  return send.apply(this,arguments);
 };
 var original=window.fetch;
 if(original)window.fetch=function(){
  var promise=original.apply(this,arguments);
  promise.then(function(response){try{if(official(response.url))response.clone().text().then(consume).catch(function(){});}catch(e){}}).catch(function(){});
  return promise;
 };
 window.HandyStationSnapshot={snapshot:function(){return {id:token+':'+revision,rows:rows.slice()};}};
})()
