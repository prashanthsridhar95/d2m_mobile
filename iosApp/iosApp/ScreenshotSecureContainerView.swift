import UIKit

/*
 * Hides its content from screenshots/screen recordings on a real device --
 * the one thing security/ScreenCapture.kt's own doc comment says Apple
 * gives no app a supported way to do. This is NOT a supported API: it
 * exploits an undocumented detail of UITextField.isSecureTextEntry --
 * iOS renders a secure text field through a protected internal layer
 * that's excluded from any screen-capture surface (the same exclusion
 * DRM video gets), and views reparented into that layer inherit the
 * exclusion. Confirmed as a known, working technique via several
 * independent sources, but every one of them also confirms it is
 * fragile and has broken across iOS betas before, requiring rework --
 * this is "best effort, watch it on every iOS release," not a permanent
 * guarantee the way Android's FLAG_SECURE (MainActivity.kt) is.
 *
 * Every failure path below degrades to "app works normally, just
 * unprotected" rather than crashing or leaving content unrenderable --
 * an iOS update silently changing the private view's structure must
 * never be the thing that breaks login for every user.
 */
final class ScreenshotSecureContainerView: UIView {
    private let secureField = UITextField()

    override init(frame: CGRect) {
        super.init(frame: frame)
        setupSecureField()
    }

    required init?(coder: NSCoder) {
        super.init(coder: coder)
        setupSecureField()
    }

    private func setupSecureField() {
        secureField.isSecureTextEntry = true
        // NOT isUserInteractionEnabled = false -- per Apple's documented
        // hit-testing algorithm, that flag makes a view's hitTest(_:with:)
        // return nil immediately without descending into its subviews at
        // all, which would silently swallow every tap meant for the real
        // Compose content reparented inside this field below. An earlier
        // version of this file had it set; ordinary taps (verified
        // on-device: a toggle button correctly switching state) work with
        // it removed. The field never gets user interaction anyway -- it's
        // covered edge-to-edge by Compose's own content, which wins
        // hit-testing as the frontmost subview, so there's no exposed
        // field surface left for a tap to land on and trigger this
        // field's own secure-entry behavior.
        addSubview(secureField)
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        secureField.frame = bounds
    }

    /*
     * Moves `contentView` (the Compose-hosted root view) inside the
     * secure field's private canvas subview, so everything Compose draws
     * inherits the screenshot exclusion.
     *
     * The canvas subview is created lazily by UIKit on the field's first
     * layout pass, not synchronously at construction -- forcing
     * layoutIfNeeded() first is what makes `subviews.first` reliably
     * non-nil here rather than only after some later, unpredictable
     * layout pass.
     */
    func hideContent(_ contentView: UIView) {
        secureField.layoutIfNeeded()
        guard let canvasView = secureField.subviews.first else {
            attachNormally(contentView)
            return
        }
        canvasView.subviews.forEach { $0.removeFromSuperview() }
        canvasView.addSubview(contentView)
        contentView.frame = canvasView.bounds
        contentView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
    }

    private func attachNormally(_ contentView: UIView) {
        addSubview(contentView)
        contentView.frame = bounds
        contentView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
    }
}
