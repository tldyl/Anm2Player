package demoMod.anm2player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.utils.Disposable;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.util.*;
import java.util.function.Consumer;

public class AnimatedActor implements Disposable {
    private Info info;
    private Map<String, Texture> spriteSheets = new HashMap<>();
    private List<Event> events = new ArrayList<>();
    private Map<String, Animation> animations = new HashMap<>();
    private Map<String, Consumer<Animation>> triggerEvent = new HashMap<>();
    private Animation curAnimation;
    private float frameTimer = 0.0F;
    public float xPosition;
    public float yPosition;
    public boolean flipX = false;
    public boolean flipY = false;
    public float scale = 1.0F;

    public AnimatedActor(String anm2Path) {
        SAXReader reader = new SAXReader();
        Document doc = null;
        try {
            doc = reader.read(Gdx.files.internal(anm2Path).read());
        } catch (DocumentException e) {
            e.printStackTrace();
        }
        if (doc != null) {
            Element root = doc.getRootElement();
            Element animationInfo = root.element("Info");
            this.info = new Info();
            info.createdBy = animationInfo.attributeValue("CreatedBy");
            info.createdOn = animationInfo.attributeValue("CreatedOn");
            info.fps = Integer.parseInt(animationInfo.attributeValue("Fps"));
            info.version = animationInfo.attributeValue("Version");
            Element content = root.element("Content");
            Element spriteSheets = content.element("Spritesheets");
            Iterator<Element> spriteSheetsIt = spriteSheets.elementIterator();
            while (spriteSheetsIt.hasNext()) {
                Element spriteSheet = spriteSheetsIt.next();
                this.spriteSheets.put(spriteSheet.attributeValue("Id"), new Texture(spriteSheet.attributeValue("Path")));
            }
            Element layers = content.element("Layers");
            Iterator<Element> layersIt = layers.elementIterator();
            Map<String, String> layers1 = new HashMap<>();
            while (layersIt.hasNext()) {
                Element layer = layersIt.next();
                layers1.put(layer.attributeValue("Id"), layer.attributeValue("SpritesheetId"));
            }
            Element events = content.element("Events");
            Iterator<Element> eventsIt = events.elementIterator();
            while (eventsIt.hasNext()) {
                Element event = eventsIt.next();
                Event event1 = new Event();
                event1.id = event.attributeValue("Id");
                event1.name = event.attributeValue("Name");
                this.events.add(event1);
            }
            Iterator<Element> animationsIt = root.element("Animations").elementIterator();
            while (animationsIt.hasNext()) {
                Element animationElement = animationsIt.next();
                Animation animation = new Animation();
                animation.frameNum = Integer.parseInt(animationElement.attributeValue("FrameNum"));
                String animationName = animationElement.attributeValue("Name");
                animation.loop = Boolean.parseBoolean(animationElement.attributeValue("Loop"));
                Element layerAnimations = animationElement.element("LayerAnimations");
                Iterator<Element> layerAnimationsIt = layerAnimations.elementIterator();
                List<LayerAnimation> layerAnimationList = new ArrayList<>();
                while (layerAnimationsIt.hasNext()) {
                    Element layerAnimationElement = layerAnimationsIt.next();
                    LayerAnimation layerAnimation = new LayerAnimation();
                    layerAnimation.id = layerAnimationElement.attributeValue("LayerId");
                    layerAnimation.spriteSheetId = layers1.get(layerAnimation.id);
                    layerAnimation.visible = Boolean.parseBoolean(layerAnimationElement.attributeValue("Visible"));
                    List<Frame> frames = new ArrayList<>();
                    Iterator<Element> layerAnimationIt = layerAnimationElement.elementIterator();
                    while (layerAnimationIt.hasNext()) {
                        Element frameElement = layerAnimationIt.next();
                        Frame frame = new Frame();
                        frame.xPosition = Float.parseFloat(frameElement.attributeValue("XPosition"));
                        frame.yPosition = Float.parseFloat(frameElement.attributeValue("YPosition"));
                        frame.xPivot = Integer.parseInt(frameElement.attributeValue("XPivot"));
                        frame.yPivot = Integer.parseInt(frameElement.attributeValue("YPivot"));
                        frame.xCrop = Integer.parseInt(frameElement.attributeValue("XCrop"));
                        frame.yCrop = Integer.parseInt(frameElement.attributeValue("YCrop"));
                        frame.width = Integer.parseInt(frameElement.attributeValue("Width"));
                        frame.height = Integer.parseInt(frameElement.attributeValue("Height"));
                        frame.xScale = Float.parseFloat(frameElement.attributeValue("XScale"));
                        frame.yScale = Float.parseFloat(frameElement.attributeValue("YScale"));
                        frame.delay = Integer.parseInt(frameElement.attributeValue("Delay"));
                        frame.visible = Boolean.parseBoolean(frameElement.attributeValue("Visible"));
                        Color tint = new Color();
                        tint.r = Float.parseFloat(frameElement.attributeValue("RedTint")) / 255.0F;
                        tint.g = Float.parseFloat(frameElement.attributeValue("GreenTint")) / 255.0F;
                        tint.b = Float.parseFloat(frameElement.attributeValue("BlueTint")) / 255.0F;
                        tint.a = Float.parseFloat(frameElement.attributeValue("AlphaTint")) / 255.0F;
                        frame.tint = tint;
                        Color colorOffset = new Color();
                        colorOffset.r = Float.parseFloat(frameElement.attributeValue("RedOffset")) / 255.0F;
                        colorOffset.g = Float.parseFloat(frameElement.attributeValue("GreenOffset")) / 255.0F;
                        colorOffset.b = Float.parseFloat(frameElement.attributeValue("BlueOffset")) / 255.0F;
                        frame.colorOffset = colorOffset;
                        frame.rotation = Integer.parseInt(frameElement.attributeValue("Rotation"));
                        if (frameElement.attributeValue("Interpolated") != null) {
                            frame.interpolated = Boolean.parseBoolean(frameElement.attributeValue("Interpolated"));
                        } else {
                            frame.interpolated = false;
                        }
                        frames.add(frame);
                    }
                    layerAnimation.frames = frames;
                    layerAnimationList.add(layerAnimation);
                }
                animation.layerAnimations = layerAnimationList;
                animation.triggers = new ArrayList<>();
                Iterator<Element> triggersIt = animationElement.element("Triggers").elementIterator();
                while (triggersIt.hasNext()) {
                    Element triggerElement = triggersIt.next();
                    Trigger trigger = new Trigger();
                    trigger.atFrame = Integer.parseInt(triggerElement.attributeValue("AtFrame"));
                    trigger.eventId = triggerElement.attributeValue("EventId");
                    animation.triggers.add(trigger);
                }
                this.animations.put(animationName, animation);
            }
        }
    }

    public void update() {
        frameTimer += Gdx.graphics.getDeltaTime();
        if (curAnimation != null && frameTimer >= 1.0F / (float) this.info.fps && !curAnimation.isDone) {
            curAnimation.xPosition = this.xPosition;
            curAnimation.yPosition = this.yPosition;
            curAnimation.update();
        }
        if (frameTimer >= 1.0F / (float) this.info.fps) {
            if (curAnimation != null) {
                for (LayerAnimation layerAnimation : curAnimation.layerAnimations) {
                    layerAnimation.currDelay++;
                }
            }
            frameTimer = 0;
        }
    }

    public void render(SpriteBatch sb) {
        if (curAnimation != null) curAnimation.render(sb);
    }

    public void setCurAnimation(String animationName) {
        this.curAnimation = this.animations.getOrDefault(animationName, null);
        curAnimation.isDone = false;
        curAnimation.xPosition = this.xPosition;
        curAnimation.yPosition = this.yPosition;
        curAnimation.frameCount = 0;
        curAnimation.update();
    }

    public void addTriggerEvent(String eventId, Consumer<Animation> event) {
        this.triggerEvent.put(eventId, event);
    }

    public Info getInfo() {
        return this.info;
    }

    public boolean isCurAnimationDone() {
        if (curAnimation != null) {
            return curAnimation.isDone;
        }
        return true;
    }

    public boolean isCurAnimation(Animation animation) {
        return curAnimation == animation;
    }

    public String getCurAnimationName() {
        for (Map.Entry<String, Animation> e : this.animations.entrySet()) {
            if (e.getValue() == curAnimation) {
                return e.getKey();
            }
        }
        return null;
    }

    @Override
    public void dispose() {
        for (Texture texture : this.spriteSheets.values()) {
            texture.dispose();
        }
    }

    private static class Info {
        String createdBy;
        String createdOn;
        int fps;
        String version;
    }

    private static class Frame {
        float xPosition;
        float yPosition;
        int xPivot;
        int yPivot;
        int xCrop;
        int yCrop;
        int width;
        int height;
        float xScale;
        float yScale;
        int delay;
        boolean visible;
        Color tint;
        Color colorOffset;
        int rotation;
        boolean interpolated;

        Frame makeCopy() {
            Frame ret = new Frame();
            ret.xPosition = this.xPosition;
            ret.yPosition = this.yPosition;
            ret.xPivot = this.xPivot;
            ret.yPivot = this.yPivot;
            ret.xCrop = this.xCrop;
            ret.yCrop = this.yCrop;
            ret.width = this.width;
            ret.height = this.height;
            ret.xScale = this.xScale;
            ret.yScale = this.yScale;
            ret.delay = this.delay;
            ret.visible = this.visible;
            ret.tint = this.tint.cpy();
            ret.colorOffset = this.colorOffset.cpy();
            ret.rotation = this.rotation;
            ret.interpolated = this.interpolated;
            return ret;
        }
    }

    public class Animation {
        int frameNum;
        int frameCount = 0;
        List<LayerAnimation> layerAnimations;
        boolean loop;
        List<Trigger> triggers;
        boolean isDone = false;
        float xPosition = 0;
        float yPosition = 0;

        void update() {
            if (!isDone) {
                for (LayerAnimation layerAnimation : layerAnimations) {
                    layerAnimation.xPosition = this.xPosition;
                    layerAnimation.yPosition = this.yPosition;
                    layerAnimation.update();
                }
            }
            for (Trigger trigger : triggers) {
                if (trigger.atFrame == frameCount) {
                    AnimatedActor.this.triggerEvent.get(trigger.eventId).accept(this);
                }
            }
            frameCount++;
            if (frameCount >= frameNum) {
                if (loop) {
                    frameCount = 0;
                } else {
                    isDone = true;
                }
            }
        }

        void render(SpriteBatch sb) {
            for (LayerAnimation layerAnimation : layerAnimations) {
                layerAnimation.render(sb);
            }
        }
    }

    private class LayerAnimation {
        String id;
        boolean visible;
        List<Frame> frames;
        Frame currFrame;
        int currFrameIndex = 0;
        int currDelay = 0;
        String spriteSheetId;
        float xPosition = 0;
        float yPosition = 0;

        void update() {
            currFrame = frames.get(currFrameIndex).makeCopy();
            if (currFrame.interpolated) {
                if (currFrameIndex < frames.size() - 1) {
                    Frame nextFrame = frames.get(currFrameIndex + 1);
                    applyInterpolation(nextFrame);
                } else if (AnimatedActor.this.curAnimation.loop && frames.size() > 0) {
                    Frame nextFrame = frames.get(0);
                    applyInterpolation(nextFrame);
                }
            }
            if (currDelay >= currFrame.delay) {
                currFrameIndex++;
                currDelay = 0;
            }
            if (currFrameIndex >= frames.size()) {
                currFrameIndex = 0;
            }
        }

        void render(SpriteBatch sb) {
            if (currFrame != null) {
                sb.setColor(currFrame.tint);
                float scaleX = (currFrame.xScale / 100.0F) * AnimatedActor.this.scale;
                float scaleY = (currFrame.yScale / 100.0F) * AnimatedActor.this.scale;
                sb.draw(AnimatedActor.this.spriteSheets.get(spriteSheetId),
                        xPosition + currFrame.xPosition * scaleX,
                        yPosition - currFrame.yPosition * scaleY,
                        currFrame.xPivot,
                        currFrame.yPivot,
                        currFrame.width,
                        currFrame.height,
                        scaleX,
                        scaleY,
                        currFrame.rotation,
                        currFrame.xCrop,
                        currFrame.yCrop,
                        currFrame.width,
                        currFrame.height,
                        AnimatedActor.this.flipX, AnimatedActor.this.flipY
                );
            }
        }

        private void applyInterpolation(Frame nextFrame) {
            currFrame.xPosition = Interpolation.exp10In.apply(currFrame.xPosition, nextFrame.xPosition, (float) currDelay / (float) currFrame.delay);
            currFrame.yPosition = Interpolation.exp10In.apply(currFrame.yPosition, nextFrame.yPosition, (float) currDelay / (float) currFrame.delay);
            currFrame.xPivot = (int) Interpolation.exp10In.apply(currFrame.xPivot, nextFrame.xPivot, (float) currDelay / (float) currFrame.delay);
            currFrame.yPivot = (int) Interpolation.exp10In.apply(currFrame.yPivot, nextFrame.yPivot, (float) currDelay / (float) currFrame.delay);
            currFrame.width = (int) Interpolation.exp10In.apply(currFrame.width, nextFrame.width, (float) currDelay / (float) currFrame.delay);
            currFrame.height = (int) Interpolation.exp10In.apply(currFrame.height, nextFrame.height, (float) currDelay / (float) currFrame.delay);
            currFrame.xScale = (int) Interpolation.exp10In.apply(currFrame.xScale, nextFrame.xScale, (float) currDelay / (float) currFrame.delay);
            currFrame.yScale = (int) Interpolation.exp10In.apply(currFrame.yScale, nextFrame.yScale, (float) currDelay / (float) currFrame.delay);
            currFrame.tint.r = (int) Interpolation.exp10In.apply(currFrame.tint.r, nextFrame.tint.r, (float) currDelay / (float) currFrame.delay);
            currFrame.tint.g = (int) Interpolation.exp10In.apply(currFrame.tint.g, nextFrame.tint.g, (float) currDelay / (float) currFrame.delay);
            currFrame.tint.b = (int) Interpolation.exp10In.apply(currFrame.tint.b, nextFrame.tint.b, (float) currDelay / (float) currFrame.delay);
            currFrame.tint.a = (int) Interpolation.exp10In.apply(currFrame.tint.a, nextFrame.tint.a, (float) currDelay / (float) currFrame.delay);
            currFrame.rotation = (int) Interpolation.exp10In.apply(currFrame.rotation, nextFrame.rotation, (float) currDelay / (float) currFrame.delay);
        }
    }

    private static class Event {
        String id;
        String name;
    }

    private static class Trigger {
        String eventId;
        int atFrame;
    }
}
