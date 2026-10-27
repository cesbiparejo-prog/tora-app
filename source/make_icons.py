import math, os
from PIL import Image, ImageDraw, ImageFont
def full(S):
    k=S/108; im=Image.new('RGBA',(S,S),(38,50,56,255)); d=ImageDraw.Draw(im)
    cx,cy,R=54*k,62*k,24*k
    def pt(a,r): return (cx+r*math.cos(math.radians(a)), cy-r*math.sin(math.radians(a)))
    d.arc([cx-R,cy-R,cx+R,cy+R],210,330,fill=(236,239,241),width=max(1,int(2.5*k)))
    for a in range(150,29,-15): d.line([pt(a,R-2*k),pt(a,R-(9 if a==90 else 6)*k)],fill=(236,239,241),width=max(1,int(1.8*k)))
    d.line([(cx,cy),pt(78,R-3*k)],fill=(105,240,174),width=max(1,int(2.6*k)))
    r=3.5*k; d.ellipse([cx-r,cy-r,cx+r,cy+r],fill=(105,240,174))
    P=[(48.5,84),(51.2,84),(52.6,79.8),(55.4,79.8),(56.8,84),(59.5,84),(55.6,74),(52.4,74)]
    d.polygon([(x*k,y*k) for x,y in P],fill=(236,239,241)); return im
def icon(size,shape):
    S=size*4; im=full(int(S*108/72)); off=(im.width-S)//2; im=im.crop((off,off,off+S,off+S))
    m=Image.new('L',(S,S),0); md=ImageDraw.Draw(m)
    if shape=='round': md.ellipse([0,0,S-1,S-1],fill=255)
    elif shape=='squircle': md.rounded_rectangle([0,0,S-1,S-1],radius=S*0.3,fill=255)
    else: md.rounded_rectangle([0,0,S-1,S-1],radius=S*0.12,fill=255)
    out=Image.new('RGBA',(S,S),(0,0,0,0)); out.paste(im,(0,0),m); return out.resize((size,size),Image.LANCZOS)
for dens,sz in [('mdpi',48),('hdpi',72),('xhdpi',96),('xxhdpi',144),('xxxhdpi',192)]:
    os.makedirs(f'mipmap-{dens}',exist_ok=True)
    icon(sz,'square').save(f'mipmap-{dens}/ic_launcher.png'); icon(sz,'round').save(f'mipmap-{dens}/ic_launcher_round.png')
pv=Image.new('RGB',(760,300),(245,245,245)); d=ImageDraw.Draw(pv)
f=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',20)
for i,sh in enumerate(['round','squircle','square']):
    ic=icon(192,sh); pv.paste(ic,(40+i*240,30),ic); d.text((40+i*240+50,240),"Afinador",fill=(33,33,33),font=f)
pv.save('/workspace/torah-app/icon-preview.png')
