(function(){
 'use strict';
 // Read only the application's visible parcel projection, never the identity card or form controls.
 if(window.top!==window || location.protocol!=='https:')return null;
 var root,kind,rows=[],capture=null;
 if(location.hostname==='m.pinduoduo.net' && location.pathname==='/mdkd/package'){
  kind='拼多多';root=document.getElementById('hc-pdd-parcels');
  if(!root || !root.dataset.ready)return {kind:kind,ready:false,rows:[]};
  if(window.HandyStationSnapshot)capture=window.HandyStationSnapshot.snapshot();
  Array.from(root.querySelectorAll('.package-code-value')).slice(0,100).forEach(function(node){
   var value=(node.textContent||'').trim();
   if(/^[A-Za-z0-9-]{3,32}$/.test(value))rows.push({code:value});
  });
 } else if(location.hostname==='pages-fast.m.taobao.com' && location.pathname==='/wow/z/uniapp/1011717/last-mile-fe/end-collect-platform/identity-code' && document.body && document.body.id==='handycode-merged'){
  kind='淘宝';root=document.getElementById('parcels-content');
  if(!root || !root.dataset.ready)return {kind:kind,ready:false,rows:[]};
  try{var frame=document.getElementById('parcels-source');if(frame&&frame.contentWindow&&frame.contentWindow.HandyStationSnapshot)capture=frame.contentWindow.HandyStationSnapshot.snapshot();}catch(e){}
  var text=(root.textContent||'').slice(0,20000),match,pattern=/(?:取件码|提货码|取货码)\s*[:：]?\s*([A-Za-z0-9-]{3,32})(?![A-Za-z0-9-])/g;
  while(rows.length<100&&(match=pattern.exec(text)))rows.push({code:match[1]});
 } else return null;
 return {kind:kind,ready:true,rows:rows,capture:capture};
})()
