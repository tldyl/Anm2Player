# Anm2Player(For libGDX)
Used to play .anm2 files from The Binding of Isaac.

用以播放来自以撒中的anm2动画文件。

## Usage
### 读取.anm2文件

    AnimatedActor animation = new AnimatedActor(".anm2文件所在的jar包内资源路径");
  
### 设置动画的绝对坐标

    animation.xPosition = 640.0F;
    animation.yPosition = 480.0F;
  
### 切换到指定动作

    animation.setCurAnimation("动作名称");
  
### 添加触发器事件(需要在anm2文件中添加相应的事件id)

    animation.addTriggerEvent("eventId", a -> {
      //这里写你的代码
    });
  
### 获得文件信息

    animation.getInfo();
  
### 判断当前动画是否播放完毕

    animation.isCurAnimationDone();
  
### 指定动画是否为当前播放的动画(暂时无用)

    animation.isCurAnimation(animation.Animation);
  
### 获得当前正在播放的动画的名称

    String name = animation.getCurAnimationName();
  
### 销毁动画

    animation.dispose();
  
### 更新

    animation.update();
  
### 渲染

    animation.render(spriteBatch);

### 获得当前正在播放的动画

    animation.getCurAnimation();

### 获得当前正在播放的动画的指定图层对象

    animation.getCurAnimation().getLayerAnimation("图层id");

### 获得当前正在播放的动画的指定空对象

    animation.getCurAnimation().getNullAnimation("空对象id");

## 示例工程

看[这里](https://github.com/tldyl/isaac-mod-extend/blob/master/src/main/java/isaacModExtend/monsters/Siren.java)
