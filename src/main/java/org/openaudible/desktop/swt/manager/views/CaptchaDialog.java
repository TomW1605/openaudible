package org.openaudible.desktop.swt.manager.views;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.openaudible.desktop.swt.gui.GUI;
import org.openaudible.desktop.swt.manager.AudibleGUI;
import org.openaudible.desktop.swt.util.shop.*;
import org.openaudible.util.Platform;

/**
 * Class displays a Dialog prompting for a username and a captcha. Will return
 * a BASE64 encoded inputValue that can be used to auth to a webserver.
 *
 * @author <a href="mailto:bpasero@rssowl.org">Benjamin Pasero </a>
 * @version 1.0.2
 */
public class CaptchaDialog extends TitleAreaDialog implements KeyListener
{

	/**
	 * Min. width of the dialog in DLUs
	 */
	private static final int dialogMinWidth = 320;

	private String dialogMessage;
	private String captchaString = "";
	private Text captcha;
	private String title;
	private Image captchaImage;

	// private Text username;

	/**
	 * Creates an input dialog with OK and Cancel buttons. Prompts for the
	 * Password. Note that the dialog will have no visual representation (no
	 * widgets) until it is told to open.
	 * <p>
	 * Note that the <code>open</code> method blocks for input dialogs.
	 * </p>
	 */
	public CaptchaDialog(Shell parentShell, String dialogTitle, String dialogMessage, String img)
	{
		super(parentShell);
		this.title = dialogTitle;
		this.dialogMessage = dialogMessage;
		this.captchaImage = PaintShop.getImage(img);
	}

	/**
	 * Returns the string typed into this input dialog.
	 *
	 * @return the input string
	 */
	public void getCaptcha()
	{
		AudibleGUI.instance.captcha = captchaString;
		//return captchaString;
	}

	public void setCaptcha(String p)
	{
		captchaString = p;
	}

	/**
	 * @see org.eclipse.jface.dialogs.Dialog#buttonPressed(int)
	 */
	@Override
	protected void buttonPressed(int buttonId)
	{

		/** Set the inputValue if OK is pressed */
		if (buttonId == IDialogConstants.OK_ID)
		{
			captchaString = captcha.getText();
		}
		else
		{
			captchaString = null;
		}

		super.buttonPressed(buttonId);
	}

	/**
	 * @see Window#configureShell(Shell)
	 */
	@Override
	protected void configureShell(Shell shell)
	{
		super.configureShell(shell);

		/** On Mac do not set Shell Image since it will change the Dock Image */
		if (Platform.isMac())
		{
			shell.setImages(PaintShop.appIcon);
		}

		shell.setText(title);
	}

	/**
	 * @see org.eclipse.jface.dialogs.Dialog#createButtonsForButtonBar(Composite)
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent)
	{

		/** Override parent, DLU dependant margins */
		((GridLayout) parent.getLayout()).marginHeight = 10;
		((GridLayout) parent.getLayout()).marginWidth = 10;

		/** Create Buttons */
		createButton(parent, IDialogConstants.OK_ID, GUI.i18n.getTranslation("BUTTON_OK"), true)
				.setFont(FontShop.instance.dialogFont());
		createButton(parent, IDialogConstants.CANCEL_ID, GUI.i18n.getTranslation("BUTTON_CANCEL"),
				false).setFont(FontShop.instance.dialogFont());
	}

	/**
	 */
	@Override
	protected Control createDialogArea(Composite parent)
	{
		// GridComposite composite = new GridComposite(parent, SWT.NONE,  )
		/** Composite to hold all components */
		Composite composite = new Composite((Composite) super.createDialogArea(parent), SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		/** Title Image */
		setTitleImage(PaintShop.getImage("baseauth.gif"));

		/** Title Message */
		setMessage(dialogMessage, IMessageProvider.INFORMATION);

		new Label(composite, SWT.NONE);

		Label msg = new Label(composite, SWT.NONE);
		msg.setText(dialogMessage);
		new Label(composite, SWT.NONE);

		new Label(composite, SWT.NONE);

		Label captchaImageLabel = new Label(parent, SWT.BORDER_SOLID);
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING);
		//gd.widthHint = imageSize;
		//gd.heightHint = imageSize;
		captchaImageLabel.setLayoutData(gd);

		captchaImageLabel.setImage(captchaImage);

		/** captcha Label */
		Label captchaLabel = new Label(composite, SWT.NONE);
		captchaLabel.setText("captcha: ");
		captchaLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
		captchaLabel.setFont(FontShop.instance.dialogFont());

		/** Password input field */
		captcha = new Text(composite, SWT.SINGLE | SWT.BORDER);
		captcha.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		captcha.setFont(FontShop.instance.dialogFont());
		// captcha.setFocus();
		captcha.setText(captchaString);

		/** Tweak Text Widget */
		WidgetShop.tweakTextWidget(captcha);

		captcha.addKeyListener(this);

		/** Spacer */
		new Label(composite, SWT.NONE);

		/** Holder for the separator to the OK and Cancel buttons */
		Composite sepHolder = new Composite(parent, SWT.NONE);
		sepHolder.setLayoutData(LayoutDataShop.createGridData(GridData.FILL_HORIZONTAL, 2));
		sepHolder.setLayout(LayoutShop.createGridLayout(1, 0, 0));

		/** Spacer */
		new Label(sepHolder, SWT.NONE);

		/** Separator */
		Label separator = new Label(sepHolder, SWT.SEPARATOR | SWT.HORIZONTAL);
		separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		return composite;
	}

	/**
	 * @see org.eclipse.jface.dialogs.Dialog#initializeBounds()
	 */
	@Override
	protected void initializeBounds()
	{
		super.initializeBounds();
		Point bestSize = getShell().computeSize(convertHorizontalDLUsToPixels(dialogMinWidth),
				SWT.DEFAULT);

		/** The URL Label might need more space than 300 DLUs */
		int bestSizeWidth = getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		if (bestSizeWidth > bestSize.x)
		{
			bestSize.x = bestSizeWidth;
		}
		Shell shell = getShell();

		getShell().setSize(bestSize.x, bestSize.y);
		LayoutShop.positionShell(getShell());
		Button ok = getButton(0);

		if (captchaString.length() == 0)
		{
			captcha.forceFocus();
		}
		else
		{
			ok.forceFocus();
		}
	}

	@Override
	public void keyReleased(KeyEvent e)
	{

	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		System.err.println("passKey: " + e.keyCode);
		if (e.keyCode == 13)
		{
			e.doit = false;
			buttonPressed(IDialogConstants.OK_ID);
		}
	}

	/**
	 * Set the layout data of the button to a GridData with appropriate widths
	 * This method was slightly modified so that it is not setting a heightHint.
	 *
	 * @param button The button to layout
	 */
	@Override
	protected void setButtonLayoutData(Button button)
	{
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		data.widthHint = Math.max(widthHint, button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		button.setLayoutData(data);
	}
}