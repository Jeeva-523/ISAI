import React, { useEffect, useState } from 'react'
import type { Song } from '@shared/models/song'
import {
  ListenTogetherService,
  RoomData,
  SyncStatus
} from '../services/ListenTogetherService'
import {
  Users,
  Check,
  LogOut,
  Play,
  Pause,
  Share2,
  Radio,
  Wifi,
  AlertTriangle,
  Volume2
} from 'lucide-react'

interface ListenTogetherModalProps {
  isOpen: boolean
  onClose: () => void
  currentSong?: Song | null
  currentTime?: number
  onPlaySong?: (song: Song, startPositionSec?: number) => void
  onTogglePlayPause?: () => void
  onSeek?: (sec: number) => void
  onNext?: () => void
  onPrev?: () => void
  needsAutoplayGesture?: boolean
  onAutoplayGestureUnlock?: () => void
}

export const ListenTogetherModal: React.FC<ListenTogetherModalProps> = ({
  isOpen,
  onClose,
  currentSong,
  currentTime = 0,
  onPlaySong: _onPlaySong,
  onTogglePlayPause: _onTogglePlayPause,
  onSeek: _onSeek,
  onNext: _onNext,
  onPrev: _onPrev,
  needsAutoplayGesture = false,
  onAutoplayGestureUnlock
}) => {
  const [room, setRoom] = useState<RoomData | null>(() => ListenTogetherService.getCurrentRoom())
  const [syncStatus, setSyncStatus] = useState<SyncStatus>(() => ListenTogetherService.getSyncStatus())
  const [joinCode, setJoinCode] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [copiedLink, setCopiedLink] = useState(false)

  const myDeviceId = ListenTogetherService.getDeviceId()
  const isHost = room ? room.hostDeviceId === myDeviceId : false

  useEffect(() => {
    if (!isOpen) return
    const unsubRoom = ListenTogetherService.subscribeRoomUpdate((r) => {
      setRoom(r)
      if (r) setErrorMessage(null)
    })
    const unsubSync = ListenTogetherService.subscribeSyncStatus((s) => {
      setSyncStatus(s)
    })
    return () => {
      unsubRoom()
      unsubSync()
    }
  }, [isOpen])

  if (!isOpen) return null

  const handleCreateRoom = async () => {
    setIsLoading(true)
    setErrorMessage(null)
    try {
      const created = await ListenTogetherService.createRoom(currentSong)
      setRoom(created)
    } catch (err: any) {
      setErrorMessage(err.message || 'Unable to create room.')
    } finally {
      setIsLoading(false)
    }
  }

  const handleJoinRoom = async (e?: React.FormEvent) => {
    if (e) e.preventDefault()
    if (!joinCode.trim()) return

    setIsLoading(true)
    setErrorMessage(null)
    try {
      const joined = await ListenTogetherService.joinRoom(joinCode.trim())
      setRoom(joined)
      setJoinCode('')
    } catch (err: any) {
      setErrorMessage(err.message || 'Unable to join room.')
    } finally {
      setIsLoading(false)
    }
  }

  const handleLeaveRoom = async () => {
    setIsLoading(true)
    try {
      await ListenTogetherService.leaveRoom()
      setRoom(null)
    } catch (err: any) {
      console.warn('[ListenTogetherModal] Leave error:', err)
    } finally {
      setIsLoading(false)
    }
  }

  const handleCopyShare = () => {
    if (!room) return
    const shareUrl = `${window.location.origin}/?room=${room.roomCode}`
    navigator.clipboard.writeText(shareUrl).then(() => {
      setCopiedLink(true)
      setTimeout(() => setCopiedLink(false), 2000)
    }).catch(() => {})
  }

  const connectedList = room?.devices
    ? Object.values(room.devices).filter((d) => d && d.connected)
    : []

  const renderStatusBadge = () => {
    if (syncStatus === 'SYNCED') {
      return (
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', color: '#C8FF00', fontSize: '13px', fontWeight: 600 }}>
          <span style={{ width: '8px', height: '8px', borderRadius: '50%', backgroundColor: '#C8FF00', boxShadow: '0 0 8px #C8FF00' }} />
          Synchronized
        </span>
      )
    }
    if (syncStatus === 'SYNCING') {
      return (
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', color: '#FFD700', fontSize: '13px', fontWeight: 600 }}>
          <span style={{ width: '8px', height: '8px', borderRadius: '50%', backgroundColor: '#FFD700' }} />
          Syncing playback...
        </span>
      )
    }
    return (
      <span style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', color: '#FF9800', fontSize: '13px', fontWeight: 600 }}>
        <AlertTriangle size={14} />
        Reconnecting...
      </span>
    )
  }

  return (
    <div
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.85)',
        backdropFilter: 'blur(10px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 10000,
        animation: 'fadeIn 0.2s ease-out'
      }}
      onClick={onClose}
    >
      <div
        style={{
          backgroundColor: '#12131A',
          border: '1px solid rgba(200, 255, 0, 0.3)',
          borderRadius: '24px',
          padding: '28px',
          width: '92%',
          maxWidth: '480px',
          color: '#FFFFFF',
          boxShadow: '0 25px 60px rgba(0, 0, 0, 0.85)',
          maxHeight: '85vh',
          overflowY: 'auto'
        }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Modal Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <div
              style={{
                width: '42px',
                height: '42px',
                borderRadius: '12px',
                background: 'linear-gradient(135deg, rgba(200, 255, 0, 0.2), rgba(0, 240, 255, 0.2))',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                border: '1px solid rgba(200, 255, 0, 0.4)'
              }}
            >
              <Radio size={22} color="#C8FF00" />
            </div>
            <div>
              <h2 style={{ margin: 0, fontSize: '20px', fontWeight: 'bold', letterSpacing: '0.3px' }}>
                Listen Together
              </h2>
              <p style={{ margin: 0, fontSize: '12px', color: '#9CA3AF' }}>
                Real-time multi-device sync (Up to 3 devices)
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            style={{
              background: 'transparent',
              border: 'none',
              color: '#9CA3AF',
              fontSize: '22px',
              cursor: 'pointer',
              padding: '4px 8px'
            }}
          >
            ✕
          </button>
        </div>

        {/* Error Alert */}
        {errorMessage && (
          <div
            style={{
              backgroundColor: 'rgba(239, 68, 68, 0.15)',
              border: '1px solid rgba(239, 68, 68, 0.4)',
              borderRadius: '12px',
              padding: '12px 16px',
              marginBottom: '16px',
              display: 'flex',
              alignItems: 'center',
              gap: '10px',
              color: '#FCA5A5',
              fontSize: '13px'
            }}
          >
            <AlertTriangle size={18} color="#EF4444" />
            <span>{errorMessage}</span>
          </div>
        )}

        {/* Autoplay blocked gesture prompt */}
        {needsAutoplayGesture && (
          <div
            style={{
              backgroundColor: 'rgba(200, 255, 0, 0.15)',
              border: '1px solid #C8FF00',
              borderRadius: '14px',
              padding: '14px',
              marginBottom: '16px',
              textAlign: 'center'
            }}
          >
            <p style={{ margin: '0 0 10px 0', fontSize: '13px', color: '#FFFFFF', fontWeight: 600 }}>
              Browser audio autoplay is restricted.
            </p>
            <button
              onClick={onAutoplayGestureUnlock}
              style={{
                backgroundColor: '#C8FF00',
                color: '#000000',
                fontWeight: 'bold',
                padding: '10px 20px',
                borderRadius: '10px',
                border: 'none',
                cursor: 'pointer',
                display: 'inline-flex',
                alignItems: 'center',
                gap: '8px',
                fontSize: '13px'
              }}
            >
              <Volume2 size={16} />
              Tap to start synchronized playback
            </button>
          </div>
        )}

        {/* Active Room View */}
        {room ? (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
            {/* Room Code Card */}
            <div
              style={{
                background: 'linear-gradient(135deg, rgba(200, 255, 0, 0.08), rgba(255, 255, 255, 0.03))',
                border: '1px solid rgba(200, 255, 0, 0.3)',
                borderRadius: '16px',
                padding: '18px'
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                <span style={{ fontSize: '11px', textTransform: 'uppercase', letterSpacing: '1px', color: '#9CA3AF' }}>
                  Room Code
                </span>
                {renderStatusBadge()}
              </div>

              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: '28px', fontWeight: '900', letterSpacing: '2px', color: '#C8FF00', fontFamily: 'monospace' }}>
                  {room.roomCode}
                </span>
                <div style={{ display: 'flex', gap: '8px' }}>
                  <button
                    onClick={handleCopyShare}
                    style={{
                      backgroundColor: copiedLink ? '#C8FF00' : 'rgba(255, 255, 255, 0.1)',
                      color: copiedLink ? '#000' : '#FFF',
                      border: 'none',
                      borderRadius: '10px',
                      padding: '8px 14px',
                      display: 'flex',
                      alignItems: 'center',
                      gap: '6px',
                      cursor: 'pointer',
                      fontSize: '12px',
                      fontWeight: 600,
                      transition: 'all 0.2s'
                    }}
                  >
                    {copiedLink ? <Check size={14} /> : <Share2 size={14} />}
                    {copiedLink ? 'Copied Link!' : 'Share'}
                  </button>
                </div>
              </div>
            </div>

            {/* Currently Playing Song in Room */}
            {room.playbackState?.song && (
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '14px',
                  backgroundColor: 'rgba(255, 255, 255, 0.04)',
                  borderRadius: '14px',
                  padding: '12px 14px',
                  border: '1px solid rgba(255, 255, 255, 0.08)'
                }}
              >
                {room.playbackState.song.artwork ? (
                  <img
                    src={room.playbackState.song.artwork}
                    alt={room.playbackState.song.title}
                    style={{ width: '48px', height: '48px', borderRadius: '10px', objectFit: 'cover' }}
                  />
                ) : (
                  <div
                    style={{
                      width: '48px',
                      height: '48px',
                      borderRadius: '10px',
                      backgroundColor: 'rgba(255, 255, 255, 0.1)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center'
                    }}
                  >
                    🎵
                  </div>
                )}
                <div style={{ flex: 1, overflow: 'hidden' }}>
                  <div style={{ fontSize: '14px', fontWeight: 600, color: '#FFF', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {room.playbackState.song.title}
                  </div>
                  <div style={{ fontSize: '12px', color: '#9CA3AF', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {room.playbackState.song.artist}
                  </div>
                </div>
                <div style={{ fontSize: '12px', color: '#C8FF00', fontWeight: 600 }}>
                  {room.playbackState.state === 'PLAYING' ? '▶ Playing' : '⏸ Paused'}
                </div>
              </div>
            )}

            {/* Host Controls Section */}
            {isHost ? (
              <div
                style={{
                  backgroundColor: 'rgba(200, 255, 0, 0.05)',
                  border: '1px solid rgba(200, 255, 0, 0.2)',
                  borderRadius: '14px',
                  padding: '14px',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between'
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <span style={{ fontSize: '13px', fontWeight: 600, color: '#C8FF00' }}>👑 You are Host</span>
                  <span style={{ fontSize: '11px', color: '#9CA3AF' }}>(Your playback syncs to everyone)</span>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  <button
                    onClick={() => {
                      if (room.playbackState?.state === 'PLAYING') {
                        ListenTogetherService.hostPause(currentTime)
                      } else {
                        ListenTogetherService.hostPlay(currentTime)
                      }
                    }}
                    style={{
                      backgroundColor: '#C8FF00',
                      color: '#000',
                      border: 'none',
                      borderRadius: '50%',
                      width: '36px',
                      height: '36px',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      cursor: 'pointer'
                    }}
                  >
                    {room.playbackState?.state === 'PLAYING' ? <Pause size={18} /> : <Play size={18} fill="#000" />}
                  </button>
                </div>
              </div>
            ) : (
              <div
                style={{
                  backgroundColor: 'rgba(255, 255, 255, 0.04)',
                  borderRadius: '14px',
                  padding: '12px 16px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '10px'
                }}
              >
                <Wifi size={18} color="#C8FF00" />
                <span style={{ fontSize: '13px', color: '#D1D5DB' }}>
                  Listening along with Host <b>{room.hostDeviceName || 'Host'}</b>
                </span>
              </div>
            )}

            {/* Connected Devices (Max 3) */}
            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
                <span style={{ fontSize: '13px', fontWeight: 600, color: '#D1D5DB', display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <Users size={16} /> Connected Devices ({connectedList.length}/3)
                </span>
              </div>

              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                {connectedList.map((dev) => {
                  const isMe = dev.deviceId === myDeviceId
                  const isDevHost = dev.deviceId === room.hostDeviceId
                  return (
                    <div
                      key={dev.deviceId}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        padding: '10px 14px',
                        backgroundColor: isMe ? 'rgba(200, 255, 0, 0.08)' : 'rgba(255, 255, 255, 0.03)',
                        borderRadius: '12px',
                        border: isMe ? '1px solid rgba(200, 255, 0, 0.3)' : '1px solid rgba(255, 255, 255, 0.06)'
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                        <span style={{ fontSize: '16px' }}>{dev.deviceType === 'ANDROID' ? '📱' : '💻'}</span>
                        <div>
                          <div style={{ fontSize: '13px', fontWeight: 600, color: isMe ? '#C8FF00' : '#FFFFFF' }}>
                            {dev.deviceName} {isMe && '(You)'}
                          </div>
                          <div style={{ fontSize: '11px', color: '#9CA3AF' }}>
                            {dev.connected ? '● Active' : '○ Offline'}
                          </div>
                        </div>
                      </div>

                      {isDevHost && (
                        <span
                          style={{
                            fontSize: '11px',
                            fontWeight: 700,
                            backgroundColor: 'rgba(200, 255, 0, 0.2)',
                            color: '#C8FF00',
                            padding: '3px 8px',
                            borderRadius: '6px'
                          }}
                        >
                          👑 HOST
                        </span>
                      )}
                    </div>
                  )
                })}
              </div>
            </div>

            {/* Leave Room Button */}
            <button
              onClick={handleLeaveRoom}
              disabled={isLoading}
              style={{
                marginTop: '10px',
                backgroundColor: 'rgba(239, 68, 68, 0.15)',
                color: '#F87171',
                border: '1px solid rgba(239, 68, 68, 0.3)',
                borderRadius: '14px',
                padding: '12px',
                fontWeight: 600,
                fontSize: '14px',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '8px'
              }}
            >
              <LogOut size={16} />
              Leave Room
            </button>
          </div>
        ) : (
          /* Not in a Room: Options to Create or Join */
          <div style={{ display: 'flex', flexDirection: 'column', gap: '22px' }}>
            {/* Create Room Button */}
            <div
              style={{
                backgroundColor: 'rgba(255, 255, 255, 0.03)',
                border: '1px solid rgba(255, 255, 255, 0.08)',
                borderRadius: '16px',
                padding: '20px',
                textAlign: 'center'
              }}
            >
              <h3 style={{ margin: '0 0 6px 0', fontSize: '16px', fontWeight: 600 }}>
                Start a Listening Room
              </h3>
              <p style={{ margin: '0 0 16px 0', fontSize: '12px', color: '#9CA3AF' }}>
                Create a private room and sync music with friends across phones and browsers.
              </p>
              <button
                onClick={handleCreateRoom}
                disabled={isLoading}
                style={{
                  width: '100%',
                  backgroundColor: '#C8FF00',
                  color: '#000000',
                  fontWeight: 'bold',
                  fontSize: '14px',
                  padding: '12px',
                  borderRadius: '12px',
                  border: 'none',
                  cursor: 'pointer',
                  boxShadow: '0 4px 15px rgba(200, 255, 0, 0.3)'
                }}
              >
                {isLoading ? 'Creating Room...' : '⚡ Create New Room'}
              </button>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              <div style={{ flex: 1, height: '1px', backgroundColor: 'rgba(255, 255, 255, 0.1)' }} />
              <span style={{ fontSize: '12px', color: '#6B7280', textTransform: 'uppercase' }}>OR JOIN</span>
              <div style={{ flex: 1, height: '1px', backgroundColor: 'rgba(255, 255, 255, 0.1)' }} />
            </div>

            {/* Join Room Form */}
            <form onSubmit={handleJoinRoom} style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <label style={{ fontSize: '13px', fontWeight: 600, color: '#D1D5DB' }}>
                Enter Room Code
              </label>
              <div style={{ display: 'flex', gap: '10px' }}>
                <input
                  type="text"
                  placeholder="e.g. ISAI-7K92"
                  value={joinCode}
                  onChange={(e) => setJoinCode(e.target.value.toUpperCase())}
                  maxLength={10}
                  style={{
                    flex: 1,
                    backgroundColor: '#1E1F2A',
                    border: '1px solid rgba(255, 255, 255, 0.15)',
                    borderRadius: '12px',
                    padding: '12px 14px',
                    color: '#FFFFFF',
                    fontSize: '15px',
                    letterSpacing: '1px',
                    fontFamily: 'monospace'
                  }}
                />
                <button
                  type="submit"
                  disabled={isLoading || !joinCode.trim()}
                  style={{
                    backgroundColor: joinCode.trim() ? '#C8FF00' : 'rgba(255, 255, 255, 0.1)',
                    color: joinCode.trim() ? '#000000' : '#6B7280',
                    border: 'none',
                    borderRadius: '12px',
                    padding: '0 20px',
                    fontWeight: 700,
                    cursor: joinCode.trim() ? 'pointer' : 'not-allowed'
                  }}
                >
                  Join
                </button>
              </div>
            </form>
          </div>
        )}
      </div>
    </div>
  )
}
