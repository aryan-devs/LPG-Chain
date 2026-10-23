import React, { useRef, useMemo, useEffect } from 'react';
import * as THREE from 'three';
import { useFrame } from '@react-three/fiber';

interface LPGCylinderProps {
  isHovered: boolean;
  isCoreActive: boolean;
  normalizedMouse: { x: number; y: number };
  scrollProgress: number;
  reducedMotion: boolean;
  onClick: () => void;
  onPointerOver: () => void;
  onPointerOut: () => void;
}

export const LPGCylinder: React.FC<LPGCylinderProps> = ({
  isHovered,
  isCoreActive,
  normalizedMouse,
  scrollProgress,
  reducedMotion,
  onClick,
  onPointerOver,
  onPointerOut,
}) => {
  const rootGroupRef = useRef<THREE.Group>(null);
  const cylinderBodyRef = useRef<THREE.Group>(null);
  const coreGlowRef = useRef<THREE.Mesh>(null);
  const scanRingRef1 = useRef<THREE.Mesh>(null);
  const scanRingRef2 = useRef<THREE.Mesh>(null);

  // Click spring impulse animation state
  const clickImpulseRef = useRef(0);

  // When isCoreActive changes, trigger a physical spring bounce
  useEffect(() => {
    clickImpulseRef.current = 1.0;
  }, [isCoreActive]);

  // Manage native cursor pointer when hovering the 3D cylinder
  useEffect(() => {
    if (typeof document !== 'undefined') {
      document.body.style.cursor = isHovered ? 'pointer' : 'auto';
      return () => {
        document.body.style.cursor = 'auto';
      };
    }
  }, [isHovered]);

  // ── Procedural Branding Texture Canvas ──
  // Creates an authentic industrial LPG cylinder silkscreen imprint matching the brand identity
  const decalTexture = useMemo(() => {
    if (typeof document === 'undefined') return null;
    const canvas = document.createElement('canvas');
    canvas.width = 1024;
    canvas.height = 512;
    const ctx = canvas.getContext('2d');
    if (!ctx) return null;

    // Completely transparent background so red painted metal shows through
    ctx.clearRect(0, 0, 1024, 512);

    // Draw stylized modern flame icon
    const flameCenterX = 512;
    const flameCenterY = 120;
    ctx.save();
    ctx.translate(flameCenterX, flameCenterY);
    ctx.beginPath();
    // Outer flame silhouette
    ctx.moveTo(0, -55);
    ctx.bezierCurveTo(24, -25, 45, 0, 42, 32);
    ctx.bezierCurveTo(40, 56, 22, 65, 0, 65);
    ctx.bezierCurveTo(-22, 65, -40, 56, -42, 32);
    ctx.bezierCurveTo(-45, 0, -24, -25, 0, -55);
    ctx.closePath();
    ctx.fillStyle = '#ffffff';
    ctx.fill();

    // Inner flame negative cutout
    ctx.beginPath();
    ctx.moveTo(0, -10);
    ctx.bezierCurveTo(12, 10, 20, 26, 18, 44);
    ctx.bezierCurveTo(14, 56, -4, 58, 0, 48);
    ctx.bezierCurveTo(6, 36, -6, 22, 0, -10);
    ctx.closePath();
    ctx.fillStyle = '#b71c1c';
    ctx.fill();
    ctx.restore();

    // Primary Brand Typography: "LPG-Chain"
    ctx.fillStyle = '#ffffff';
    ctx.font = '900 78px -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif';
    ctx.textAlign = 'center';
    ctx.letterSpacing = '1px';
    ctx.fillText('LPG-Chain', 512, 245);

    // Brand Motto: "SAFE • SMART • CONNECTED"
    ctx.fillStyle = 'rgba(255, 255, 255, 0.95)';
    ctx.font = 'bold 24px -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif';
    ctx.letterSpacing = '5px';
    ctx.fillText('SAFE  •  SMART  •  CONNECTED', 512, 295);

    // Domestic Cylinder Specifications (IS 3196 compliant standard)
    ctx.fillStyle = 'rgba(255, 255, 255, 0.8)';
    ctx.font = '600 20px monospace';
    ctx.letterSpacing = '2px';
    ctx.fillText('NET WT: 14.2 KG   |   TARE: 15.3 KG', 512, 365);

    ctx.fillStyle = 'rgba(255, 255, 255, 0.65)';
    ctx.font = '500 16px -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif';
    ctx.letterSpacing = '1px';
    ctx.fillText('CERTIFIED IS:3196 PART 1  •  IoT TELEMETRY SEALED', 512, 405);

    // Discrete barcode & serial marking
    ctx.fillStyle = 'rgba(255, 255, 255, 0.75)';
    const startX = 372;
    const barWidths = [3, 1, 4, 2, 1, 3, 2, 4, 1, 2, 3, 1, 4, 2, 2, 3, 1, 4, 3, 1, 2, 4, 1, 3, 2, 4, 2, 1, 3];
    let currX = startX;
    barWidths.forEach((w) => {
      ctx.fillRect(currX, 435, w * 2.8, 24);
      currX += (w + 1.8) * 4.6;
    });

    const texture = new THREE.CanvasTexture(canvas);
    texture.anisotropy = 8;
    return texture;
  }, []);

  // ── Materials ──
  // High-fidelity powder-coated steel safety red
  const steelRedMaterial = useMemo(() => {
    return new THREE.MeshStandardMaterial({
      color: '#b71c1c', // Authentic Indian domestic LPG safety red
      roughness: 0.32,
      metalness: 0.18,
    });
  }, []);

  const steelDarkMaterial = useMemo(() => {
    return new THREE.MeshStandardMaterial({
      color: '#7f1d1d', // Weld seam and base rim shade
      roughness: 0.42,
      metalness: 0.28,
    });
  }, []);

  const brassValveMaterial = useMemo(() => {
    return new THREE.MeshStandardMaterial({
      color: '#d4af37', // Machined brass valve assembly
      roughness: 0.26,
      metalness: 0.88,
    });
  }, []);

  const blueSealMaterial = useMemo(() => {
    return new THREE.MeshStandardMaterial({
      color: '#0284c7', // Safety tamper-evident plastic seal cap
      roughness: 0.32,
      metalness: 0.08,
    });
  }, []);

  // Animation frame loop
  useFrame((state, delta) => {
    if (!rootGroupRef.current || !cylinderBodyRef.current) return;

    // Decay click spring impulse
    if (clickImpulseRef.current > 0) {
      clickImpulseRef.current = THREE.MathUtils.lerp(clickImpulseRef.current, 0, delta * 6);
    }

    // ── Floating & Rotation ──
    if (!reducedMotion) {
      // Gentle idle vertical bobbing
      const floatY = Math.sin(state.clock.elapsedTime * 1.1) * 0.04;
      rootGroupRef.current.position.y = floatY;

      // Base idle rotation + cursor follow
      const targetRotY =
        state.clock.elapsedTime * 0.16 +
        normalizedMouse.x * 0.32 +
        scrollProgress * 1.2;
      const targetRotX = -normalizedMouse.y * 0.18 + 0.06;

      cylinderBodyRef.current.rotation.y = THREE.MathUtils.lerp(
        cylinderBodyRef.current.rotation.y,
        targetRotY,
        delta * 3.5
      );
      cylinderBodyRef.current.rotation.x = THREE.MathUtils.lerp(
        cylinderBodyRef.current.rotation.x,
        targetRotX,
        delta * 3.5
      );
    } else {
      // Static presentation for reduced motion
      cylinderBodyRef.current.rotation.y = THREE.MathUtils.lerp(
        cylinderBodyRef.current.rotation.y,
        0.25,
        delta * 4
      );
      cylinderBodyRef.current.rotation.x = THREE.MathUtils.lerp(
        cylinderBodyRef.current.rotation.x,
        0.04,
        delta * 4
      );
    }

    // ── Scaling Reactions ──
    // Hover: 5-8% scale bump
    // Click: up to 10% spring bump
    // Scroll: subtle zoom into Core
    const hoverScale = isHovered ? 1.06 : 1.0;
    const clickScale = 1.0 + clickImpulseRef.current * 0.08;
    const scrollScale = 1.0 + scrollProgress * 0.1;
    const targetScale = hoverScale * clickScale * scrollScale;

    rootGroupRef.current.scale.lerp(
      new THREE.Vector3(targetScale, targetScale, targetScale),
      delta * 6
    );

    // ── Internal Telemetry Core Animation ──
    if (coreGlowRef.current) {
      const targetOpacity = isCoreActive || scrollProgress > 0.35 ? 0.75 : 0.0;
      const mat = coreGlowRef.current.material as THREE.MeshBasicMaterial;
      mat.opacity = THREE.MathUtils.lerp(mat.opacity, targetOpacity, delta * 5);
    }

    if (scanRingRef1.current && (isCoreActive || scrollProgress > 0.3)) {
      scanRingRef1.current.rotation.z += delta * 1.5;
      scanRingRef1.current.scale.setScalar(
        1.0 + Math.sin(state.clock.elapsedTime * 3.5) * 0.05
      );
    }
    if (scanRingRef2.current && (isCoreActive || scrollProgress > 0.3)) {
      scanRingRef2.current.rotation.z -= delta * 1.2;
    }
  });

  return (
    <group ref={rootGroupRef} position={[0, -0.15, 0]}>
      <group
        ref={cylinderBodyRef}
        onClick={(e) => {
          e.stopPropagation();
          onClick();
        }}
        onPointerOver={(e) => {
          e.stopPropagation();
          onPointerOver();
        }}
        onPointerOut={(e) => {
          e.stopPropagation();
          onPointerOut();
        }}
      >
        {/* ── 1. Main Cylindrical Metal Body ── */}
        <mesh position={[0, 0, 0]} material={steelRedMaterial} castShadow receiveShadow>
          <cylinderGeometry args={[0.9, 0.9, 1.35, 64, 1, false]} />
        </mesh>

        {/* ── 2. Upper Shoulder (Deep-Drawn Hemispherical Dome) ── */}
        <mesh position={[0, 0.675, 0]} material={steelRedMaterial} castShadow>
          <sphereGeometry args={[0.9, 64, 32, 0, Math.PI * 2, 0, Math.PI * 0.48]} />
        </mesh>

        {/* ── 3. Lower Base Dome ── */}
        <mesh position={[0, -0.675, 0]} rotation={[Math.PI, 0, 0]} material={steelRedMaterial} castShadow>
          <sphereGeometry args={[0.9, 64, 32, 0, Math.PI * 2, 0, Math.PI * 0.44]} />
        </mesh>

        {/* ── 4. Middle Circumferential Welded Seam & Bead ── */}
        <mesh position={[0, 0, 0]} rotation={[Math.PI / 2, 0, 0]} material={steelDarkMaterial}>
          <torusGeometry args={[0.906, 0.024, 16, 64]} />
        </mesh>

        {/* ── 5. Lower Base Stand Ring / Foot Ring with Ventilation Cutouts ── */}
        <group position={[0, -1.18, 0]}>
          {/* Main Foot Ring Cylinder */}
          <mesh material={steelDarkMaterial} castShadow receiveShadow>
            <cylinderGeometry args={[0.84, 0.86, 0.34, 48, 1, true]} />
          </mesh>
          {/* Base Ring Rim Rolled Bead */}
          <mesh position={[0, -0.16, 0]} rotation={[Math.PI / 2, 0, 0]} material={steelDarkMaterial}>
            <torusGeometry args={[0.86, 0.022, 16, 48]} />
          </mesh>
          {/* Base Ring Ventilation Slots (4 drainage slots around base ring) */}
          {[0, Math.PI / 2, Math.PI, (3 * Math.PI) / 2].map((angle, i) => (
            <mesh
              key={`vent-${i}`}
              position={[
                Math.sin(angle) * 0.852,
                -0.02,
                Math.cos(angle) * 0.852,
              ]}
              rotation={[0, angle, 0]}
            >
              <boxGeometry args={[0.16, 0.08, 0.04]} />
              <meshBasicMaterial color="#1e293b" />
            </mesh>
          ))}
        </group>

        {/* ── 6. Top Protective Collar / Valve Guard Shroud with 3 Stay Plates ── */}
        <group position={[0, 1.14, 0]}>
          {/* Top Tubular Hand-Ring */}
          <mesh position={[0, 0.24, 0]} rotation={[Math.PI / 2, 0, 0]} material={steelRedMaterial} castShadow>
            <torusGeometry args={[0.66, 0.038, 16, 48]} />
          </mesh>

          {/* 3 Vertical Stay Plates welded at 120° angles to upper dome */}
          {[0, (2 * Math.PI) / 3, (4 * Math.PI) / 3].map((angle, index) => (
            <group key={`stay-${index}`} rotation={[0, angle, 0]}>
              <mesh position={[0.64, 0.03, 0]} rotation={[0, 0, -0.1]} material={steelRedMaterial} castShadow>
                <boxGeometry args={[0.042, 0.42, 0.16]} />
              </mesh>
            </group>
          ))}

          {/* Two Ergonomic Hand Grip Handles between the stay plates */}
          <mesh position={[0.66, 0.08, 0]} rotation={[0, 0, Math.PI / 2]} material={steelDarkMaterial}>
            <cylinderGeometry args={[0.028, 0.028, 0.22, 16]} />
          </mesh>
          <mesh position={[-0.66, 0.08, 0]} rotation={[0, 0, Math.PI / 2]} material={steelDarkMaterial}>
            <cylinderGeometry args={[0.028, 0.028, 0.22, 16]} />
          </mesh>
        </group>

        {/* ── 7. Valve Mechanism (Center Top under collar) ── */}
        <group position={[0, 1.02, 0]}>
          {/* Brass Valve Body with Hexagonal Base */}
          <mesh material={brassValveMaterial} castShadow>
            <cylinderGeometry args={[0.11, 0.13, 0.32, 24]} />
          </mesh>
          {/* Valve Handwheel / Knob */}
          <mesh position={[0, 0.2, 0]} material={brassValveMaterial}>
            <cylinderGeometry args={[0.18, 0.18, 0.06, 24]} />
          </mesh>
          {/* Gas Outlet Spout */}
          <mesh position={[0.14, 0.05, 0]} rotation={[0, 0, Math.PI / 2]} material={brassValveMaterial}>
            <cylinderGeometry args={[0.06, 0.06, 0.16, 16]} />
          </mesh>
          {/* Blue Tamper-Evident Safety Cap with Pull-Seal */}
          <mesh position={[0.22, 0.05, 0]} rotation={[0, 0, Math.PI / 2]} material={blueSealMaterial}>
            <cylinderGeometry args={[0.072, 0.072, 0.09, 20]} />
          </mesh>
        </group>

        {/* ── 8. Curved Silkscreen Branding Decal ── */}
        {/* Conforms naturally to the cylinder radius without flat rectangular clipping */}
        {decalTexture && (
          <mesh position={[0, 0.02, 0]} rotation={[0, 0, 0]}>
            <cylinderGeometry args={[0.906, 0.906, 0.82, 64, 1, true, -Math.PI * 0.38, Math.PI * 0.76]} />
            <meshBasicMaterial
              map={decalTexture}
              transparent
              opacity={0.96}
              depthWrite={false}
              side={THREE.DoubleSide}
            />
          </mesh>
        )}

        {/* ── 9. Internal Digital Intelligence Layer (Core) ── */}
        {/* Glow inner telemetry core */}
        <mesh ref={coreGlowRef} position={[0, 0, 0]}>
          <cylinderGeometry args={[0.915, 0.915, 1.36, 48, 1, true]} />
          <meshBasicMaterial
            color={isCoreActive ? '#0ea5e9' : '#f97316'}
            transparent
            opacity={0}
            wireframe={true}
            side={THREE.DoubleSide}
            depthWrite={false}
          />
        </mesh>

        {/* Active Telemetry Rings around Seam */}
        {(isCoreActive || scrollProgress > 0.25) && (
          <group position={[0, 0, 0]}>
            <mesh ref={scanRingRef1} rotation={[Math.PI / 2, 0, 0]}>
              <ringGeometry args={[0.96, 1.04, 64]} />
              <meshBasicMaterial
                color="#0284c7"
                transparent
                opacity={0.8}
                side={THREE.DoubleSide}
                depthWrite={false}
              />
            </mesh>

            <mesh ref={scanRingRef2} rotation={[Math.PI / 2, 0, 0]}>
              <ringGeometry args={[1.08, 1.13, 64]} />
              <meshBasicMaterial
                color="#f97316"
                transparent
                opacity={0.55}
                side={THREE.DoubleSide}
                depthWrite={false}
              />
            </mesh>

            {/* Vertical Telemetry Beacons */}
            <mesh position={[0, 1.48, 0]}>
              <cylinderGeometry args={[0.02, 0.02, 0.4, 16]} />
              <meshBasicMaterial color="#38bdf8" transparent opacity={0.7} />
            </mesh>
          </group>
        )}
      </group>
    </group>
  );
};
