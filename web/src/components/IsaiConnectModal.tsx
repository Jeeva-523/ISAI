import React, { useEffect, useState } from 'react'
import type { Song } from '@shared/models/song'
import { IsaiConnectService, DeviceInfo, PlaybackStateSync } from '../services/IsaiConnectService'

interface IsaiConnectModalProps {
  isOpen: boolean
  onClose: () => void
  currentSong?: Song | null
  currentTime?: number
  onTransferToLocal?: () => void
  onTransferToRemote?: (targetDeviceId: string) => void
}

export const IsaiConnectModal: React.FC<IsaiConnectModalProps> = ({
  isOpen,
  onClose,
  currentSong,
  currentTime = 0,
  onTransferToLocal,
  onTransferToRemote
}) => {
  const [devices, setDevices] = useState<DeviceInfo[]>([])
  const [playbackState, setPlaybackState] = useState<PlaybackStateSync | null>(null)
  const myDeviceId = IsaiConnectService.getMyDeviceId()

  useEffect(() => {
    if (!isOpen) return
    const unsubDevices = IsaiConnectService.subscribeDevices(setDevices)
    const unsubState = IsaiConnectService.subscribePlaybackState(setPlaybackState)
    return () => {
      unsubDevices()
      unsubState()
    }
  }, [isOpen])

  if (!isOpen) return null

  const currentActiveDeviceId = playbackState?.currentDeviceId || myDeviceId

  const getPlatformIcon = (platform: string) => {
    switch (platform) {
      case 'android': return '📱'
      case 'ios': return '📱'
      case 'windows': return '💻'
      case 'mac': return '💻'
      case 'tablet': return '📱'
      default: return '🖥️'
    }
  }

  const formatPresence = (device: DeviceInfo) => {
    const diffMs = Date.now() - (device.lastActiveAt || 0)
    if (device.isActive && diffMs < 35000) {
      return <span style={{ color: '#C8FF00' }}>🟢 Active now</span>
    }
    if (diffMs < 300000) {
      const mins = Math.max(1, Math.floor(diffMs / 60000))
      return <span style={{ color: '#FFC107' }}>🟡 Active {mins} min ago</span>
    }
    return <span style={{ color: '#6C757D' }}>⚫ Offline</span>
  }

  const handleSelectDevice = (targetDeviceId: string) => {
    if (targetDeviceId === myDeviceId) {
      IsaiConnectService.transferPlaybackToDevice(myDeviceId)
      onTransferToLocal?.()
    } else {
      // Transfer to Remote Device (e.g. Phone):
      const activeSong = currentSong || (playbackState?.currentSongId ? {
        videoId: playbackState.currentSongId,
        title: playbackState.currentTitle,
        channelTitle: playbackState.currentArtist,
        thumbnailUrl: playbackState.currentArtwork,
        audioUrl: playbackState.currentAudioUrl,
        durationFormatted: '3:30',
        durationMs: playbackState.durationMs || 210000,
        viewCountFormatted: ''
      } : null)

      const posMs = Math.round((currentTime || (playbackState?.positionMs ? playbackState.positionMs / 1000 : 0)) * 1000)

      if (activeSong) {
        // 1. Send PLAY_SONG command targeted specifically to the phone/remote device
        IsaiConnectService.sendCommand('PLAY_SONG', {
          song: activeSong,
          positionMs: posMs,
          targetDeviceId
        })

        // 2. Claim target device ownership and sync song state in RTDB
        IsaiConnectService.updatePlaybackState({
          currentDeviceId: targetDeviceId,
          currentSongId: activeSong.videoId,
          currentTitle: activeSong.title,
          currentArtist: activeSong.channelTitle,
          currentArtwork: activeSong.thumbnailUrl,
          currentAudioUrl: activeSong.audioUrl,
          isPlaying: true,
          positionMs: posMs
        })
      } else {
        IsaiConnectService.transferPlaybackToDevice(targetDeviceId)
      }

      onTransferToRemote?.(targetDeviceId)
    }
  }

  return (
    <div style={{
      position: 'fixed',
      top: 0, left: 0, right: 0, bottom: 0,
      backgroundColor: 'rgba(0,0,0,0.85)',
      backdropFilter: 'blur(8px)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 9999,
      animation: 'fadeIn 0.2s ease-out'
    }} onClick={onClose}>
      <div style={{
        backgroundColor: '#15151C',
        border: '1px solid rgba(200, 255, 0, 0.25)',
        borderRadius: '20px',
        padding: '28px',
        width: '90%',
        maxWidth: '460px',
        color: '#FFFFFF',
        boxShadow: '0 20px 50px rgba(0, 0, 0, 0.8)',
        maxHeight: '80vh',
        overflowY: 'auto'
      }} onClick={e => e.stopPropagation()}>
        
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <span style={{ fontSize: '24px' }}>🎧</span>
            <div>
              <h2 style={{ margin: 0, fontSize: '20px', fontWeight: 'bold', color: '#FFFFFF' }}>ISAI Connect</h2>
              <p style={{ margin: 0, fontSize: '12px', color: '#C8FF00' }}>⚡ Syncing account: <b>{IsaiConnectService.getUserEmail() || 'Guest Account'}</b></p>
            </div>
          </div>
          <button onClick={onClose} style={{
            background: 'none',
            border: 'none',
            color: '#A5A5AE',
            fontSize: '24px',
            cursor: 'pointer',
            padding: '4px'
          }}>✕</button>
        </div>

        {/* Current Active Device Status Banner */}
        <div style={{
          backgroundColor: '#0B0B0F',
          borderRadius: '12px',
          padding: '14px 16px',
          border: '1px solid rgba(200, 255, 0, 0.3)',
          marginBottom: '24px',
          display: 'flex',
          alignItems: 'center',
          gap: '12px'
        }}>
          <span style={{ fontSize: '20px', animation: 'pulse 1.5s infinite' }}>⚡</span>
          <div>
            <div style={{ fontSize: '11px', textTransform: 'uppercase', letterSpacing: '1px', color: '#C8FF00', fontWeight: 'bold' }}>
              Current Audio Output
            </div>
            <div style={{ fontSize: '14px', fontWeight: '600', color: '#FFFFFF', marginTop: '2px' }}>
              {currentActiveDeviceId === myDeviceId
                ? `🟢 Playing on This Device (${IsaiConnectService.getMyDeviceName()})`
                : `📱 Playing on Remote Device`}
            </div>
          </div>
        </div>

        {/* Section: This Device */}
        <div style={{ marginBottom: '20px' }}>
          <div style={{ fontSize: '12px', textTransform: 'uppercase', color: '#A5A5AE', fontWeight: 'bold', marginBottom: '10px' }}>
            This Device
          </div>
          <div 
            onClick={() => handleSelectDevice(myDeviceId)}
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              padding: '14px 16px',
              backgroundColor: currentActiveDeviceId === myDeviceId ? 'rgba(200, 255, 0, 0.12)' : '#1C1C26',
              border: currentActiveDeviceId === myDeviceId ? '1.5px solid #C8FF00' : '1px solid rgba(255,255,255,0.08)',
              borderRadius: '14px',
              cursor: 'pointer',
              transition: 'all 0.2s ease'
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
              <span style={{ fontSize: '22px' }}>🖥️</span>
              <div>
                <div style={{ fontWeight: '600', color: currentActiveDeviceId === myDeviceId ? '#C8FF00' : '#FFFFFF' }}>
                  {IsaiConnectService.getMyDeviceName()}
                </div>
                <div style={{ fontSize: '12px', color: '#A5A5AE', marginTop: '2px' }}>
                  This Web Browser
                </div>
              </div>
            </div>
            {currentActiveDeviceId === myDeviceId && (
              <span style={{ backgroundColor: '#C8FF00', color: '#0B0B0F', fontSize: '12px', fontWeight: 'bold', padding: '4px 10px', borderRadius: '20px' }}>
                Active Player
              </span>
            )}
          </div>
        </div>

        {/* Section: Available Devices */}
        <div>
          {(() => {
            const availableDevices = devices.filter(d => d.deviceId !== myDeviceId && IsaiConnectService.isDeviceOnline(d))
            return (
              <>
                <div style={{ fontSize: '12px', textTransform: 'uppercase', color: '#A5A5AE', fontWeight: 'bold', marginBottom: '10px' }}>
                  Available Devices ({availableDevices.length})
                </div>

                {availableDevices.length === 0 ? (
                  <div style={{
                    textAlign: 'center',
                    padding: '24px',
                    backgroundColor: '#1C1C26',
                    borderRadius: '14px',
                    color: '#A5A5AE',
                    fontSize: '13px'
                  }}>
                    Log into your ISAI account on Android or another browser to switch playback seamlessly!
                  </div>
                ) : (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
                    {availableDevices.map((device) => {
                      const isSelected = currentActiveDeviceId === device.deviceId
                      return (
                        <div
                          key={device.deviceId}
                          onClick={() => handleSelectDevice(device.deviceId)}
                          style={{
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'space-between',
                            padding: '14px 16px',
                            backgroundColor: isSelected ? 'rgba(200, 255, 0, 0.12)' : '#1C1C26',
                            border: isSelected ? '1.5px solid #C8FF00' : '1px solid rgba(255,255,255,0.08)',
                            borderRadius: '14px',
                            cursor: 'pointer',
                            transition: 'all 0.2s ease'
                          }}
                        >
                          <div style={{ display: 'flex', alignItems: 'center', gap: '14px' }}>
                            <span style={{ fontSize: '22px' }}>{getPlatformIcon(device.platform)}</span>
                            <div>
                              <div style={{ fontWeight: '600', color: isSelected ? '#C8FF00' : '#FFFFFF' }}>
                                {device.deviceName}
                              </div>
                              <div style={{ fontSize: '12px', color: '#A5A5AE', marginTop: '2px' }}>
                                {formatPresence(device)}
                              </div>
                            </div>
                          </div>
                          {isSelected && (
                            <span style={{ backgroundColor: '#C8FF00', color: '#0B0B0F', fontSize: '12px', fontWeight: 'bold', padding: '4px 10px', borderRadius: '20px' }}>
                              Active Player
                            </span>
                          )}
                        </div>
                      )
                    })}
                  </div>
                )}
              </>
            )
          })()}
        </div>

      </div>
    </div>
  )
}
