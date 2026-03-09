using System.Collections;
using TMPro;
using UnityEngine;

namespace PoC.Scripts
{
    public class SpeechBubble : MonoBehaviour
    {
        [SerializeField] private Canvas bubbleCanvas;
        [SerializeField] private TMP_Text bubbleText;
        [SerializeField] private CanvasGroup canvasGroup;
        [SerializeField] private float displayDuration = 4f;
        [SerializeField] private float fadeDuration = 0.5f;

        private Coroutine _hideCoroutine;
        private Camera _mainCam;

        private void Start()
        {
            _mainCam = Camera.main;
            bubbleCanvas.worldCamera = _mainCam;
            canvasGroup.alpha = 0f;
        }

        private void LateUpdate()
        {
            if (_mainCam == null) return;
            bubbleCanvas.transform.forward = _mainCam.transform.forward;
        }

        /// <summary>
        /// Compose -> UnityBridge.sendMessage("PocCharacter", "ShowBubble", text)
        /// </summary>
        public void ShowBubble(string text)
        {
            if (_hideCoroutine != null) StopCoroutine(_hideCoroutine);

            bubbleText.text = text;
            canvasGroup.alpha = 1f;
            _hideCoroutine = StartCoroutine(AutoHide());
        }

        private IEnumerator AutoHide()
        {
            yield return new WaitForSeconds(displayDuration);

            float elapsed = 0f;
            while (elapsed < fadeDuration)
            {
                elapsed += Time.deltaTime;
                canvasGroup.alpha = 1f - (elapsed / fadeDuration);
                yield return null;
            }

            canvasGroup.alpha = 0f;
        }
    }
}