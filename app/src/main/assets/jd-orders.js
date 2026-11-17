(function () {
 'use strict';
 if (location.protocol !== 'https:' || !/(^|\.)jd\.com$/i.test(location.hostname) || window.top !== window) return null;
 if (!window.HandyJdOrders) {
  var state={captured:false,orders:[],revision:0};
  function clean(value,max){return typeof value==='string'?value.slice(0,max):'';}
  function code(text) {
   var m=String(text).match(/(?:取件码|提货码|取货码|凭码|自提码)[:：\s]*([A-Za-z0-9-]{3,12})(?![A-Za-z0-9-])/);
   return m?m[1]:'';
  }
  function events(value) {
   if(!Array.isArray(value))return [];
   return value.slice(0,200).map(function(item,index){
    if(!item || typeof item!=='object')return null;
    var text=clean(item.wlStateDesc,1000).trim(), raw=item.createTime, epoch=NaN, shown='';
    if(!text)return null;
    if(typeof raw==='number' && Number.isFinite(raw)) {
     epoch=raw>=1e12?raw:raw>=1e9?raw*1000:NaN;
     if(!Number.isFinite(epoch)||epoch>8640000000000000)return null;
     shown=new Date(epoch).toISOString();
    } else if(typeof raw==='string') {
     shown=raw.slice(0,80).trim();
     // The JD H5 time without an offset is a China local time; invalid dates stay unparsed.
     var match=shown.match(/^(\d{4})-(\d{2})-(\d{2})[ T](\d{2}):(\d{2})(?::(\d{2}))?$/);
     if(match){
      var parts=match.slice(1).map(function(v){return Number(v||0);});
      var utc=Date.UTC(parts[0],parts[1]-1,parts[2],parts[3],parts[4],parts[5]);
      var check=new Date(utc);
      if(check.getUTCFullYear()===parts[0]&&check.getUTCMonth()===parts[1]-1&&check.getUTCDate()===parts[2]
          &&check.getUTCHours()===parts[3]&&check.getUTCMinutes()===parts[4]&&check.getUTCSeconds()===parts[5])epoch=utc-8*3600000;
     } else if(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d+)?(?:Z|[+-]\d{2}:\d{2})$/.test(shown))epoch=Date.parse(shown);
    }
    if(!Number.isFinite(epoch))return null;
    return {time:shown,epoch:epoch,text:text,index:index};
   }).filter(Boolean).sort(function(a,b){return b.epoch-a.epoch||a.index-b.index;});
  }
  function consume(root) {
   if(!root||!root.body||!Array.isArray(root.body.orderList))return;
   var rows=[],seen=Object.create(null);
   root.body.orderList.slice(0,100).forEach(function(o){
    if(!o||!o.orderId)return;
    var status=clean(o.orderStatusInfo&&o.orderStatusInfo.orderStatusName,100);
    if(/取消|关闭|退款/.test(status))return;
    var p=o.progressInfo||{},text=clean(p.content,1500),tip=clean(p.tip,500);
    // Only filter explicit instant-delivery labels, not ordinary JD delivery.
    if(/即时配送|小时达|同城急送/.test(status+' '+text+' '+tip))return;
    var goods=Array.isArray(o.wareInfoList)?o.wareInfoList[0]:null;
    var tracking=clean(p.carriageId,100).trim(), carrier=clean(p.carrier,100).trim();
    var identity=String(o.orderId)+'\u0000'+tracking;
    if(seen[identity])return;
    rows.push({id:String(o.orderId).slice(0,80),name:clean(goods&&goods.wareName,180)||'京东订单',
     status:status,detail:text,code:code(text+' '+status+' '+tip),tracking:tracking,carrier:carrier,
     timeline:events(p.tracePointVOList)});
    seen[identity]=true;
   });
   state.orders=rows;state.captured=true;state.revision++;
  }
  function response(text) {
   if(typeof text!=='string'||text.length>2097152||text.indexOf('"orderList"')<0)return;
   try{consume(JSON.parse(text));}catch(ignore){}
  }
  var send=XMLHttpRequest.prototype.send;
  XMLHttpRequest.prototype.send=function(){
   this.addEventListener('load',function(){
    try{
     var url=new URL(this.responseURL,location.href);
     if(url.protocol!=='https:'||!/(^|\.)jd\.com$/i.test(url.hostname))return;
     if(this.responseType==='json')consume(this.response);
     else if(!this.responseType||this.responseType==='text')response(this.responseText);
    }catch(ignore){}
   });
   return send.apply(this,arguments);
  };
  var fetchOriginal=window.fetch;
  if(fetchOriginal)window.fetch=function(){
   var promise=fetchOriginal.apply(this,arguments);
   promise.then(function(res){
    try{
     var url=new URL(res.url,location.href);
     if(url.protocol==='https:'&&/(^|\.)jd\.com$/i.test(url.hostname))
      res.clone().text().then(response).catch(function(){});
    }catch(ignore){}
   }).catch(function(){});
   return promise;
  };
  // Fallback only extracts bounded order blocks; never reads login form values.
  function domOrders(){
   var nodes=document.querySelectorAll('a[href*="orderId"],a[href*="orderid"]'),rows=[],seen=Object.create(null);
   for(var i=0;i<nodes.length&&rows.length<50;i++){
    var link=nodes[i],url;
    try{url=new URL(link.href,location.href);}catch(ignore){continue;}
    if(!/(^|\.)jd\.com$/i.test(url.hostname))continue;
    var id=url.searchParams.get('orderId')||url.searchParams.get('orderid');
    if(!id||seen[id])continue;
    var block=link.closest('li,[class*="order-item"],[class*="orderItem"],[class*="order_item"]');
    if(!block)continue;
    var text=(block.innerText||'').slice(0,2000),value=code(text);
    if(!value||/取消|关闭|退款/.test(text))continue;
    seen[id]=true;rows.push({id:id.slice(0,80),name:'京东待取订单',status:'待取件',detail:text.slice(0,500),code:value});
   }
   return rows;
  }
  window.HandyJdOrders={snapshot:function(){
   var login=/(^|\.)plogin\.m\.jd\.com$/i.test(location.hostname)||/\/login(?:\/|$)/i.test(location.pathname);
   var problem='',empty=false;
   var notice=!login&&document.querySelector('[class*="index-module__errorCon"]');
   if(notice&&notice.getClientRects().length){
    var message=(notice.innerText||'').slice(0,500).replace(/\s/g,'');
    empty=/(?:暂无|还没有|没有|未找到|暂未查询到)(?:符合条件的|相关的?|任何)?(?:订单|包裹)|(?:订单|包裹)空空如也/.test(message)
     && !/异常|失败|繁忙|通道|重试|再试/.test(message);
    if(!empty&&/大促异常火爆|快速通道|服务繁忙/.test(message))problem='busy';
    else if(!empty&&/异常|失败|出错|错误|无法加载|重新加载|重试|再试/.test(message))problem='unavailable';
   }
   var rows=state.captured?state.orders:domOrders();
   return {href:location.href,login:login,problem:problem,ready:!login&&!problem&&(state.captured||rows.length>0||empty),orders:login||problem||empty?[]:rows};
  }};
 }
 return window.HandyJdOrders.snapshot();
})()
