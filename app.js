const $=id=>document.getElementById(id);let books,lex,TR,cache={},st={b:'Gen',c:1};
const NAMES=[[/\bJehová\b/g,'YHWH'],[/\bJEHOVÁ\b/g,'YHWH'],[/\bDios\b/g,'Elohim'],[/\bMoisés\b/g,'Moshé'],[/\bAarón\b/g,'Aharón'],[/\bAbraham\b/g,'Avraham'],[/\bAbram\b/g,'Avram'],[/\bIsaac\b/g,'Yitsjak'],[/\bJacob\b/g,'Yaakov'],[/\bIsrael\b/g,'Yisrael'],[/\bJosé\b/g,'Yosef'],[/\bSara\b/g,'Sarah'],[/\bRebeca\b/g,'Rivkah'],[/\bRaquel\b/g,'Rajel'],[/\bLea\b/g,'Leah'],[/\bEva\b/g,'Javah'],[/\bNoé\b/g,'Noaj'],[/\bJudá\b/g,'Yehudah'],[/\bLeví\b/g,'Levi'],[/\bBenjamín\b/g,'Binyamín'],[/\bEfraín\b/g,'Efrayim'],[/\bManasés\b/g,'Menashé'],[/\bJosué\b/g,'Yehoshúa'],[/\bEgipto\b/g,'Mitsrayim'],[/\bFaraón\b/g,'Paró'],[/\bJerusalem\b/g,'Yerushalayim'],[/\bSinaí\b/g,'Sinai'],[/\bsábado\b/g,'shabat'],[/\bEsaú\b/g,'Esav'],[/\bIsmael\b/g,'Yishmael'],[/\bCanaán\b/g,'Kenaán'],[/\bSeñor\b/g,'Adonai']];
const nm=t=>$('names').checked?NAMES.reduce((s,[r,x])=>s.replace(r,x),t):t;
const lat=s=>s.normalize('NFD').replace(/[^a-z]/gi,'').toLowerCase();const bare=s=>s.replace(/[\u0591-\u05C7\u05BE\/]/g,'');const strip=s=>s.replace(/[\u0591-\u05AF\u05BD\u05C0\u05C3-\u05C6]/g,'');
function tr(h){h=strip(h);let o='';for(let i=0;i<h.length;i++){let c=h[i],n=h[i+1];if('בכפ'.includes(c)&&n=='\u05bc'){o+={'ב':'b','כ':'k','פ':'p'}[c];i++;continue}if(c=='ש'&&n=='\u05c2'){o+='s';i++;continue}if(c=='\u05bc'||c=='\u05c1')continue;if(c=='ו'&&n=='\u05b9'){o+='o';i++;continue}if(c=='ו'&&n=='\u05bc'){o+='u';i++;continue}o+=TR[c]??''}return o}
const gloss=s=>lex[s]?.es?.[0]||(lex[s]?.k||'').split(/[,;]/)[0]||'';
async function J(u){return (await fetch(u)).json()}
async function book(b){return cache[b]??=await J('data/'+b+'.json')}
async function init(){[books,lex,TR]=await Promise.all([J('data/books.json'),J('data/lexicon.json'),J('data/translit.json')]);
$('book').innerHTML=books.map(b=>`<option value="${b.id}">${b.es} (${b.he})</option>`).join('');
const h=location.hash.slice(1).split('.');if(h[0]&&books.find(b=>b.id==h[0])){st.b=h[0];st.c=+h[1]||1}
$('book').onchange=()=>{st.b=$('book').value;st.c=1;render()};$('chap').onchange=()=>{st.c=+$('chap').value;render()};
$('prev').onclick=()=>move(-1);$('next').onclick=()=>move(1);document.querySelectorAll('[name=mode]').forEach(r=>r.onchange=render);$('names').onchange=render;
$('go').onclick=search;$('q').onkeydown=e=>e.key=='Enter'&&search();$('dictbtn').onclick=()=>openDict('');$('dclose').onclick=()=>$('dict').hidden=true;$('dq').oninput=()=>dsearch($('dq').value);render()}
function move(d){let i=books.findIndex(b=>b.id==st.b),bk=books[i];st.c+=d;if(st.c<1){if(i>0){st.b=books[i-1].id;st.c=books[i-1].chapters}else st.c=1}else if(st.c>bk.chapters){if(i<4){st.b=books[i+1].id;st.c=1}else st.c=bk.chapters}render()}
async function render(){const bk=books.find(b=>b.id==st.b),d=await book(st.b);$('book').value=st.b;
$('chap').innerHTML=Array.from({length:bk.chapters},(_,i)=>`<option ${i+1==st.c?'selected':''}>${i+1}</option>`).join('');location.hash=st.b+'.'+st.c;
const mode=document.querySelector('[name=mode]:checked').value,vs=d[st.c-1];let h=`<h2>${bk.es} ${st.c} <small>· ${bk.he}</small></h2>`;
for(const v of vs){const he=v.he.map(w=>`<span class="hw" data-s="${w[1]}">${w[0]}</span>`).join(' ');
if(mode=='par')h+=`<div class="vs" id="v${v.v}"><div class="he"><span class="n">${v.v}</span>${he}</div><div class="rv"><span class="n">${v.v}</span>${nm(v.rv)}</div></div>`;
else if(mode=='rv')h+=`<p class="rv" id="v${v.v}"><span class="n">${v.v}</span>${nm(v.rv)}</p>`;
else h+=`<details id="v${v.v}" ${innerWidth>700?'open':''}><summary><span class="n">${v.v}</span>${nm(v.rv)}</summary><div class="il">${v.he.map(w=>`<div class="w" data-s="${w[1]}"><div class="h">${w[0]}</div><small>${tr(w[0])}</small><small class="s">${w[1]||''}</small><small class="g">${nm(gloss(w[1]))}</small></div>`).join('')}</div></details>`}
$('main').innerHTML=h;$('main').querySelectorAll('[data-s]').forEach(e=>e.onclick=()=>openDict(e.dataset.s));scrollTo(0,0)}
function ent(k){const e=lex[k];if(!e)return'';return`<div class="ent"><b>${k}</b> <span class="lm">${e.l}</span> <i>${e.x}</i> <span class="tag">(${e.p})</span>
${e.es.length?`<div><b>Español (RV1909):</b> ${e.es.join(', ')}</div>`:''}<div><b>Definición</b> <span class="tag">[inglés, Strong]</span>: ${e.d}</div><div><b>KJV:</b> ${e.k}</div><div class="tag">${e.e}</div></div>`}
function openDict(s){$('dict').hidden=false;$('dq').value=s;dsearch(s)}
function dsearch(q){q=q.trim();if(!q){$('dres').innerHTML='<p>Escribe un número Strong, palabra hebrea, transliteración o palabra en español/inglés.</p>';return}
if(/^h?\d+$/i.test(q)){$('dres').innerHTML=ent('H'+parseInt(q.replace(/h/i,'')))||'No encontrado';return}
const ql=q.toLowerCase(),r=[];for(const k in lex){const e=lex[k];if(bare(e.l).includes(bare(q))&&bare(q)||lat(e.x).includes(lat(q))||lat(e.x).replace(/y/g,'').includes(lat(q).replace(/y/g,''))||e.es.some(x=>x.includes(ql))||e.d.toLowerCase().includes(ql)){r.push(k);if(r.length>=60)break}}
$('dres').innerHTML=r.map(ent).join('')||'Sin resultados'}
async function search(){const q=$('q').value.trim();if(!q)return;const hits=[];const sn=/^h\d+$/i.test(q)?'H'+parseInt(q.slice(1)):null,ql=strip(q).toLowerCase();
for(const b of books){const d=await book(b.id);d.forEach((ch,ci)=>ch.forEach(v=>{if(sn?v.he.some(w=>w[1]==sn):(nm(v.rv).toLowerCase().includes(ql)||bare(v.he.map(w=>w[0]).join(' ')).includes(bare(q))))hits.push([b,ci+1,v])}))}
$('main').innerHTML=`<h2>${hits.length} resultados para «${q}»</h2>`+hits.slice(0,500).map(([b,c,v])=>`<div class="hit" data-b="${b.id}" data-c="${c}" data-v="${v.v}"><b>${b.es} ${c}:${v.v}</b> ${nm(v.rv)}</div>`).join('');
$('main').querySelectorAll('.hit').forEach(e=>e.onclick=async()=>{st.b=e.dataset.b;st.c=+e.dataset.c;await render();document.getElementById('v'+e.dataset.v)?.scrollIntoView()})}
init();
if('serviceWorker' in navigator)navigator.serviceWorker.register('sw.js');
